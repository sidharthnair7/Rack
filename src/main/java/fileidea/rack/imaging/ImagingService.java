package fileidea.rack.imaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.ImageKind;
import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.common.TaskStatus;
import fileidea.rack.common.Vendor;
import fileidea.rack.config.ImagingProperties;
import fileidea.rack.config.PerfectCorpProperties;
import fileidea.rack.intake.Item;
import fileidea.rack.intake.ItemRepository;
import fileidea.rack.integration.perfectcorp.PerfectCorpClient;
import fileidea.rack.integration.perfectcorp.PerfectCorpClient.TaskResult;
import fileidea.rack.integration.storage.ImageStorage;
import fileidea.rack.task.TaskEndpoints;
import fileidea.rack.task.TaskOrchestrator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ImagingService {

    private static final Logger log = LoggerFactory.getLogger(ImagingService.class);

    private final ItemRepository items;
    private final ImageAssetRepository assets;
    private final ImageStorage storage;
    private final PerfectCorpClient perfectCorp;
    private final PerfectCorpProperties perfectCorpProps;
    private final ImagingProperties flags;
    private final TaskOrchestrator tasks;

    public ImagingService(
            ItemRepository items,
            ImageAssetRepository assets,
            ImageStorage storage,
            PerfectCorpClient perfectCorp,
            PerfectCorpProperties perfectCorpProps,
            ImagingProperties flags,
            TaskOrchestrator tasks
    ) {
        this.items = items;
        this.assets = assets;
        this.storage = storage;
        this.perfectCorp = perfectCorp;
        this.perfectCorpProps = perfectCorpProps;
        this.flags = flags;
        this.tasks = tasks;
    }

    @Transactional
    public List<ImageAsset> photograph(Long itemId) {
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("item " + itemId));
        byte[] current = storage.read(storage.keyFromPublicUrl(item.getSourceImageUrl()));
        String workingFileId = null;

        if (!perfectCorpProps.hasKey()) {
            log.info("no Perfect Corp key — catalog image is the original for item {}", itemId);
            save(item, ImageKind.STUDIO, item.getSourceImageUrl(), TaskStatus.SUCCESS, null);
        } else {
            if (flags.backgroundRemoval()) {
                StageResult cut = runStage(item, ImageKind.CUTOUT, "sod", current, workingFileId, Map.of());
                current = cut.bytes();
                workingFileId = cut.dstId();
            }
            if (flags.lighting()) {
                StageResult relit = runStage(item, ImageKind.RELIT, "lighting", current, workingFileId, Map.of());
                current = relit.bytes();
                workingFileId = relit.dstId();
            }
            if (flags.enhance()) {
                StageResult enhanced = runStage(item, ImageKind.ENHANCED, "enhance", current, workingFileId, Map.of("scale", 2));
                current = enhanced.bytes();
                workingFileId = enhanced.dstId();
            }
            if (flags.studio()) {
                StageResult studio = runStage(item, ImageKind.STUDIO, "ai-studio", current, workingFileId, Map.of());
                current = studio.bytes();
                workingFileId = studio.dstId();
            } else {
                saveBytes(item, ImageKind.STUDIO, current);
            }
            if (flags.tryOn()) {
                tryOn(item, current, workingFileId);
            }
        }

        item.setStatus(ItemStatus.IMAGED);
        items.save(item);
        tasks.enqueue(item, Vendor.LLM, TaskEndpoints.PUBLISH, TaskEndpoints.publishKey(item.getId()));
        return assets.findByItemId(itemId);
    }

    public String catalogUrl(Long itemId) {
        List<ImageAsset> all = assets.findByItemId(itemId);
        return firstUrl(all, ImageKind.ON_MODEL)
                .or(() -> firstUrl(all, ImageKind.STUDIO))
                .or(() -> firstUrl(all, ImageKind.ENHANCED))
                .or(() -> firstUrl(all, ImageKind.RELIT))
                .or(() -> firstUrl(all, ImageKind.CUTOUT))
                .or(() -> firstUrl(all, ImageKind.ORIGINAL))
                .orElse(null);
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void tryOn(Item item, byte[] garment, String garmentFileId) {
        long started = System.nanoTime();
        try {
            // The garment is uploaded to cloth-v4 directly rather than reusing the file id that
            // came back from the enhance stage. Chaining a previous stage's dstId works for the
            // src_file_id of the next editing stage, but cloth-v4 reads ref_file_id as "the
            // garment to transfer", and handing it an id minted by a different service produced a
            // render of a plausible generic jacket instead of the jacket that was photographed.
            // One extra upload per item, and the before/after actually shows the same garment.
            String refId = perfectCorp.upload("cloth-v4", garment, "garment.jpg");
            if (!perfectCorpProps.hasOwnModel()) {
                log.warn("rack.perfectcorp.model-url is not set, so try-on is running against Perfect "
                        + "Corp's stock sample model. Generate a synthetic model with AI Avatar "
                        + "Generator and set the URL before publishing or recording.");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("src_file_url", perfectCorpProps.resolvedModelUrl());
            body.put("ref_file_id", refId);
            body.put("garment_category", garmentCategory(item));
            String taskId = perfectCorp.submit("cloth-v4", body);
            TaskResult result = wait(item, ImageKind.ON_MODEL, "cloth-v4", taskId);
            if (result != null && result.resultUrl() != null) {
                persistDownload(item, ImageKind.ON_MODEL, result.resultUrl(), taskId);
            }
            log.info("imaging item {}: cloth-v4 try-on finished in {} ms", item.getId(), millisSince(started));
        } catch (Exception e) {
            log.warn("try-on failed for item {} — falling back to catalog: {}", item.getId(), e.getMessage());
            saveBytes(item, ImageKind.ON_MODEL, garment);
        }
    }

    private StageResult runStage(
            Item item,
            ImageKind kind,
            String service,
            byte[] input,
            String srcFileId,
            Map<String, Object> extra
    ) {
        long started = System.nanoTime();
        try {
            String fileId = srcFileId != null ? srcFileId : perfectCorp.upload(service, input, kind.name().toLowerCase() + ".jpg");
            Map<String, Object> body = new LinkedHashMap<>(extra);
            body.put("src_file_id", fileId);
            String taskId = perfectCorp.submit(service, body);
            TaskResult result = wait(item, kind, service, taskId);
            if (result == null || result.resultUrl() == null) {
                saveBytes(item, kind, input);
                return new StageResult(input, srcFileId);
            }
            byte[] next = persistDownload(item, kind, result.resultUrl(), taskId);
            log.info("imaging item {}: {} finished in {} ms", item.getId(), service, millisSince(started));
            return new StageResult(next, result.dstId());
        } catch (Exception e) {
            log.warn("{} failed for item {} — keeping previous image: {}", service, item.getId(), e.getMessage());
            saveBytes(item, kind, input);
            return new StageResult(input, srcFileId);
        }
    }

    /** Hard ceiling per stage. Five stages x five minutes was longer than any demo take. */
    private static final long STAGE_TIMEOUT_MS = 120_000L;

    /**
     * Perfect Corp jobs typically finish in seconds, so poll tightly at first and back off.
     * The previous fixed 15s floor meant a job that finished in two seconds still cost fifteen,
     * which multiplied across four stages and every item in the batch.
     */
    private TaskResult wait(Item item, ImageKind kind, String service, String taskId) throws InterruptedException {
        ImageAsset pending = save(item, kind, null, TaskStatus.IN_FLIGHT, taskId);
        long deadline = System.currentTimeMillis() + STAGE_TIMEOUT_MS;
        long backoffMs = 500L;
        while (System.currentTimeMillis() < deadline) {
            TaskResult result = perfectCorp.poll(service, taskId);
            if (result.success()) {
                pending.setStatus(TaskStatus.SUCCESS);
                assets.save(pending);
                return result;
            }
            if (!result.running()) {
                log.warn("{} task {} for item {} ended as '{}'", service, taskId, item.getId(), result.status());
                pending.setStatus(TaskStatus.ERROR);
                assets.save(pending);
                return null;
            }
            Thread.sleep(Math.min(backoffMs, Math.max(0, deadline - System.currentTimeMillis())));
            // Capped at 2s rather than 4s: four stages each overshooting a finished job by up to
            // four seconds is most of the wait the user actually feels.
            backoffMs = Math.min(backoffMs * 2, 2_000L);
        }
        log.warn("{} task {} for item {} timed out after {}ms", service, taskId, item.getId(), STAGE_TIMEOUT_MS);
        pending.setStatus(TaskStatus.ERROR);
        assets.save(pending);
        return null;
    }

    /**
     * Flattens a transparent result onto white before it is stored.
     *
     * Background removal returns a PNG with an alpha channel, and every byte of the original
     * background is still sitting in the RGB channels underneath that mask. Writing those bytes
     * straight to a .jpg produced a file that looked untouched: the transparency was discarded and
     * the bedspread came back. It cost us the stage, because measuring the saved file showed the
     * garment unchanged and the obvious conclusion was that the API had done nothing.
     *
     * Compositing here means the pixels the mask marked as background are actually gone, and the
     * catalog image is the garment on white rather than the garment on a rug. Anything without an
     * alpha channel passes through untouched.
     */
    private static byte[] flattenOntoWhite(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null || !src.getColorModel().hasAlpha()) {
                return bytes;
            }
            BufferedImage flat = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = flat.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, flat.getWidth(), flat.getHeight());
            g.drawImage(src, 0, 0, null);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(flat, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            // A stage that cannot be flattened is still better than no stage: keep the original.
            log.warn("could not flatten transparency, storing the response as-is: {}", e.getMessage());
            return bytes;
        }
    }

    private byte[] persistDownload(Item item, ImageKind kind, String url, String taskId) {
        byte[] bytes = flattenOntoWhite(perfectCorp.download(url));
        String stored = storage.store(
                "items/" + item.getId() + "/" + kind.name().toLowerCase() + ".jpg",
                bytes,
                "image/jpeg"
        );
        save(item, kind, stored, TaskStatus.SUCCESS, taskId);
        return bytes;
    }

    private void saveBytes(Item item, ImageKind kind, byte[] bytes) {
        String stored = storage.store(
                "items/" + item.getId() + "/" + kind.name().toLowerCase() + ".jpg",
                bytes,
                "image/jpeg"
        );
        save(item, kind, stored, TaskStatus.SUCCESS, null);
    }

    private ImageAsset save(Item item, ImageKind kind, String url, TaskStatus status, String taskId) {
        ImageAsset asset = assets.findByItemId(item.getId()).stream()
                .filter(a -> a.getKind() == kind)
                .findFirst()
                .orElseGet(ImageAsset::new);
        asset.setItem(item);
        asset.setKind(kind);
        asset.setUrl(url);
        asset.setStatus(status);
        asset.setPerfectCorpTaskId(taskId);
        return assets.save(asset);
    }

    private static java.util.Optional<String> firstUrl(List<ImageAsset> all, ImageKind kind) {
        return all.stream()
                .filter(a -> a.getKind() == kind && a.getUrl() != null)
                .map(ImageAsset::getUrl)
                .findFirst();
    }

    static String garmentCategory(Item item) {
        String raw = ((item.getCategory() == null ? "" : item.getCategory()) + " "
                + (item.getIdentifiedType() == null ? "" : item.getIdentifiedType()))
                .toLowerCase(Locale.ROOT);
        if (raw.contains("dress") || raw.contains("jumpsuit") || raw.contains("gown")) {
            return "full_body";
        }
        if (raw.contains("jean") || raw.contains("pant") || raw.contains("trouser")
                || raw.contains("skirt") || raw.contains("short") || raw.contains("bottom")) {
            return "lower_body";
        }
        // Anything else is treated as upper body: it is the value the try-on service is most
        // likely to accept, and a wrong-but-valid category degrades to a poor render, whereas an
        // invented one ("auto", "outerwear") is rejected outright and loses the stage entirely.
        return "upper_body";
    }

    private record StageResult(byte[] bytes, String dstId) {
    }
}
