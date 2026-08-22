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
                StageResult enhanced = runStage(item, ImageKind.RELIT, "enhance", current, workingFileId, Map.of("scale", 2));
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
                .or(() -> firstUrl(all, ImageKind.RELIT))
                .or(() -> firstUrl(all, ImageKind.CUTOUT))
                .or(() -> firstUrl(all, ImageKind.ORIGINAL))
                .orElse(null);
    }

    private void tryOn(Item item, byte[] garment, String garmentFileId) {
        try {
            String refId = garmentFileId != null
                    ? garmentFileId
                    : perfectCorp.upload("cloth-v4", garment, "garment.jpg");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("src_file_url", perfectCorpProps.modelUrl());
            body.put("ref_file_id", refId);
            body.put("garment_category", garmentCategory(item));
            String taskId = perfectCorp.submit("cloth-v4", body);
            TaskResult result = wait(item, ImageKind.ON_MODEL, "cloth-v4", taskId);
            if (result != null && result.resultUrl() != null) {
                persistDownload(item, ImageKind.ON_MODEL, result.resultUrl(), taskId);
            }
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
            return new StageResult(next, result.dstId());
        } catch (Exception e) {
            log.warn("{} failed for item {} — keeping previous image: {}", service, item.getId(), e.getMessage());
            saveBytes(item, kind, input);
            return new StageResult(input, srcFileId);
        }
    }

    private TaskResult wait(Item item, ImageKind kind, String service, String taskId) throws InterruptedException {
        ImageAsset pending = save(item, kind, null, TaskStatus.IN_FLIGHT, taskId);
        for (int i = 0; i < 20; i++) {
            TaskResult result = perfectCorp.poll(service, taskId);
            int sleepSec = result.pollingIntervalSec() == null ? 15 : Math.max(5, result.pollingIntervalSec());
            if (result.success()) {
                pending.setStatus(TaskStatus.SUCCESS);
                assets.save(pending);
                return result;
            }
            if (!result.running()) {
                pending.setStatus(TaskStatus.ERROR);
                assets.save(pending);
                return null;
            }
            Thread.sleep(sleepSec * 1000L);
        }
        pending.setStatus(TaskStatus.ERROR);
        assets.save(pending);
        return null;
    }

    private byte[] persistDownload(Item item, ImageKind kind, String url, String taskId) {
        byte[] bytes = perfectCorp.download(url);
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
        if (raw.contains("jean") || raw.contains("pant") || raw.contains("trouser") || raw.contains("skirt")) {
            return "lower_body";
        }
        if (raw.contains("shoe") || raw.contains("sneaker") || raw.contains("boot")) {
            return "shoes";
        }
        if (raw.contains("jacket") || raw.contains("coat") || raw.contains("hoodie")) {
            return "outerwear";
        }
        if (raw.contains("shirt") || raw.contains("tee") || raw.contains("top") || raw.contains("sweater")) {
            return "upper_body";
        }
        return "auto";
    }

    private record StageResult(byte[] bytes, String dstId) {
    }
}
