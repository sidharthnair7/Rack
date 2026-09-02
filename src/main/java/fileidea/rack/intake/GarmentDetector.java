package fileidea.rack.intake;

import fileidea.rack.config.PerfectCorpProperties;
import fileidea.rack.integration.perfectcorp.PerfectCorpClient;
import fileidea.rack.integration.perfectcorp.PerfectCorpClient.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Finds the separate garments in one photograph.
 *
 * <p>Rack's pitch has always been "photograph the pile", and the product did one garment per
 * photo. That gap was not cosmetic: a seller with a full closet still had to shoot, upload and
 * wait once per piece, which is most of the work the product claims to remove.
 *
 * <p>The insight is that the hard half is already solved by a vendor we call anyway. Perfect
 * Corp's {@code sod} returns the photo with its background removed as an RGBA PNG, and the alpha
 * channel of that result is a foreground mask for <em>everything</em> in the frame. Two jumpers on
 * a bed come back as two islands of opaque pixels separated by transparency. So the detection
 * problem reduces to finding connected regions in that mask, which is plain arithmetic over a
 * bitmap and needs no new vendor, no model, and no API key we do not already hold.
 *
 * <p>Each region is cropped out of the <em>original</em> photo rather than the cutout, because
 * every downstream stage expects real pixels: Lens identifies better with context, and the imaging
 * stages run their own background removal.
 */
@Component
public class GarmentDetector {

    private static final Logger log = LoggerFactory.getLogger(GarmentDetector.class);

    /** Long edge the mask is scaled to before labelling. Keeps a 12MP photo to ~300k pixels. */
    static final int ANALYSIS_WIDTH = 480;

    /** A region smaller than this share of the frame is a shadow, a label, or a stray sock. */
    static final double MIN_AREA_FRACTION = 0.015;

    /** More than this and it is a wardrobe shot, not a pile: fall back to the whole frame. */
    static final int MAX_GARMENTS = 6;

    /** Grown by this share of its size so a crop does not clip sleeves at the boundary. */
    static final double PADDING = 0.04;

    /**
     * How far to shave the mask boundary, in analysis pixels, when trying to separate garments
     * that touch. Tried smallest first: the least aggressive erosion that splits the pile is the
     * one least likely to also break a single garment at a narrow waist or a thin strap.
     */
    static final int[] ERODE_RADII = {1, 2, 3};

    private static final long TIMEOUT_MS = 60_000L;

    private final PerfectCorpClient perfectCorp;
    private final PerfectCorpProperties props;

    public GarmentDetector(PerfectCorpClient perfectCorp, PerfectCorpProperties props) {
        this.perfectCorp = perfectCorp;
        this.props = props;
    }

    /**
     * Split one photo into one image per garment.
     *
     * <p>Returns a single-element list holding the original bytes whenever splitting is not
     * possible or not warranted - no API key, one garment, an unreadable image, a vendor failure.
     * Detection is an enhancement to intake and must never be the reason an upload fails, so every
     * error path here degrades to the behaviour the product had before it existed.
     */
    public List<byte[]> split(byte[] original) {
        // Separated from the key check below on purpose. These two used to share a branch that
        // ended in List.of(original), and List.of rejects null - so the one line written to make
        // a null degrade gracefully was the line that would have thrown on it.
        if (original == null || original.length == 0) {
            return List.of();
        }
        if (!props.hasKey()) {
            return List.of(original);
        }
        try {
            BufferedImage photo = ImageIO.read(new ByteArrayInputStream(original));
            if (photo == null) {
                return List.of(original);
            }
            BufferedImage mask = foregroundMask(original);
            if (mask == null) {
                return List.of(original);
            }
            List<Rectangle> regions = regions(mask, photo.getWidth(), photo.getHeight());
            if (regions.size() < 2) {
                return List.of(original);
            }
            List<byte[]> crops = new ArrayList<>();
            for (Rectangle box : regions) {
                byte[] crop = encode(photo.getSubimage(box.x, box.y, box.width, box.height));
                if (crop != null) {
                    crops.add(crop);
                }
            }
            if (crops.size() < 2) {
                return List.of(original);
            }
            log.info("detected {} garments in one photo", crops.size());
            return crops;
        } catch (Exception e) {
            log.warn("garment detection failed, treating the photo as a single item: {}", e.getMessage());
            return List.of(original);
        }
    }

    /** Runs {@code sod} and returns its RGBA result, whose alpha channel is the foreground mask. */
    private BufferedImage foregroundMask(byte[] original) throws Exception {
        String fileId = perfectCorp.upload("sod", original, "pile.jpg");
        String taskId = perfectCorp.submit("sod", Map.of("src_file_id", fileId));
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        long backoffMs = 500L;
        while (System.currentTimeMillis() < deadline) {
            TaskResult result = perfectCorp.poll("sod", taskId);
            if (result.success() && result.resultUrl() != null) {
                return ImageIO.read(new ByteArrayInputStream(perfectCorp.download(result.resultUrl())));
            }
            if (!result.running()) {
                log.warn("sod ended as '{}' during detection", result.status());
                return null;
            }
            Thread.sleep(Math.min(backoffMs, Math.max(0, deadline - System.currentTimeMillis())));
            backoffMs = Math.min(backoffMs * 2, 2_000L);
        }
        log.warn("sod timed out during detection");
        return null;
    }

    /**
     * Bounding boxes of the separate foreground regions, in original-photo coordinates.
     *
     * <p>Pure arithmetic over the mask, so it is unit tested directly rather than through the
     * vendor. Works on a downscaled copy: labelling every pixel of a 12MP phone photo would
     * allocate tens of megabytes on a 1GB box to answer a question that a 480px-wide thumbnail
     * answers identically.
     */
    static List<Rectangle> regions(BufferedImage mask, int originalWidth, int originalHeight) {
        if (mask == null || mask.getWidth() == 0 || mask.getHeight() == 0) {
            return List.of();
        }
        if (!mask.getColorModel().hasAlpha()) {
            // Nothing was cut out, so there is no foreground to separate.
            return List.of();
        }

        int w = Math.min(ANALYSIS_WIDTH, mask.getWidth());
        int h = Math.max(1, (int) Math.round(mask.getHeight() * (w / (double) mask.getWidth())));
        boolean[] solid = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            int sourceY = (int) ((y + 0.5) * mask.getHeight() / h);
            for (int x = 0; x < w; x++) {
                int sourceX = (int) ((x + 0.5) * mask.getWidth() / w);
                int alpha = (mask.getRGB(Math.min(sourceX, mask.getWidth() - 1),
                        Math.min(sourceY, mask.getHeight() - 1)) >>> 24);
                solid[y * w + x] = alpha > 128;
            }
        }

        int minPixels = (int) Math.max(1, MIN_AREA_FRACTION * w * h);
        List<Rectangle> found = new ArrayList<>();
        List<Integer> areas = new ArrayList<>();
        label(solid, w, h, minPixels, found, areas);

        // Garments touching in the frame are one shape in the mask, and people photograph piles by
        // dropping clothes on a bed, not by laying them out on a grid. Where the mask says "one
        // thing", try again on an eroded copy: shaving the boundary breaks the narrow bridge where
        // a sleeve rests on a hem, while the bodies of the garments survive it.
        //
        // Deliberately only attempted when plain labelling found nothing to split. Erosion cannot
        // then turn a correct single-garment answer into a wrong multi-garment one - the worst it
        // does is fail to help, and the photo is handled exactly as it was before.
        if (found.size() < 2) {
            for (int radius : ERODE_RADII) {
                List<Rectangle> eroded = new ArrayList<>();
                List<Integer> erodedAreas = new ArrayList<>();
                label(erode(solid, w, h, radius), w, h, minPixels, eroded, erodedAreas);
                if (eroded.size() >= 2 && eroded.size() <= MAX_GARMENTS) {
                    // Grow each box back by what was shaved off, so the crop is not missing the
                    // edge of the garment that erosion removed.
                    found = new ArrayList<>();
                    for (Rectangle r : eroded) {
                        found.add(new Rectangle(r.x - radius, r.y - radius,
                                r.width + 2 * radius, r.height + 2 * radius));
                    }
                    areas = erodedAreas;
                    break;
                }
            }
        }

        if (found.size() < 2 || found.size() > MAX_GARMENTS) {
            return List.of();
        }

        // Biggest first, so if anything is dropped later it is the least significant piece.
        // Copied to locals because the erosion pass above may have replaced both lists.
        final List<Rectangle> regions = found;
        final List<Integer> sizes = areas;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt((Integer i) -> sizes.get(i)).reversed());

        List<Rectangle> scaled = new ArrayList<>();
        double scaleX = originalWidth / (double) w;
        double scaleY = originalHeight / (double) h;
        for (int i : order) {
            Rectangle r = regions.get(i);
            int padX = (int) Math.round(r.width * PADDING);
            int padY = (int) Math.round(r.height * PADDING);
            int x = (int) Math.floor((r.x - padX) * scaleX);
            int y = (int) Math.floor((r.y - padY) * scaleY);
            int rw = (int) Math.ceil((r.width + 2 * padX) * scaleX);
            int rh = (int) Math.ceil((r.height + 2 * padY) * scaleY);
            x = Math.max(0, x);
            y = Math.max(0, y);
            rw = Math.min(rw, originalWidth - x);
            rh = Math.min(rh, originalHeight - y);
            if (rw > 0 && rh > 0) {
                scaled.add(new Rectangle(x, y, rw, rh));
            }
        }
        return scaled;
    }

    /**
     * Flood-fills every solid region, collecting the bounding box and pixel count of each one
     * bigger than {@code minPixels}. Iterative rather than recursive: a garment spanning a hundred
     * thousand pixels would overflow the stack.
     */
    private static void label(boolean[] solid, int w, int h, int minPixels,
                              List<Rectangle> boxes, List<Integer> areas) {
        boolean[] seen = new boolean[w * h];
        for (int start = 0; start < solid.length; start++) {
            if (!solid[start] || seen[start]) {
                continue;
            }
            Deque<Integer> queue = new ArrayDeque<>();
            queue.push(start);
            seen[start] = true;
            int minX = w, maxX = -1, minY = h, maxY = -1, count = 0;
            while (!queue.isEmpty()) {
                int p = queue.pop();
                int px = p % w, py = p / w;
                count++;
                minX = Math.min(minX, px); maxX = Math.max(maxX, px);
                minY = Math.min(minY, py); maxY = Math.max(maxY, py);
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        int nx = px + dx, ny = py + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                            continue;
                        }
                        int n = ny * w + nx;
                        if (solid[n] && !seen[n]) {
                            seen[n] = true;
                            queue.push(n);
                        }
                    }
                }
            }
            if (count >= minPixels) {
                boxes.add(new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1));
                areas.add(count);
            }
        }
    }

    /** Keeps a pixel only if every pixel within {@code radius} of it is also solid. */
    private static boolean[] erode(boolean[] solid, int w, int h, int radius) {
        boolean[] out = new boolean[solid.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!solid[y * w + x]) {
                    continue;
                }
                boolean keep = true;
                for (int dy = -radius; dy <= radius && keep; dy++) {
                    for (int dx = -radius; dx <= radius && keep; dx++) {
                        int nx = x + dx, ny = y + dy;
                        // Outside the frame counts as solid: a garment running off the edge of the
                        // photo should not be eaten away from that edge inwards.
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                            continue;
                        }
                        if (!solid[ny * w + nx]) {
                            keep = false;
                        }
                    }
                }
                out[y * w + x] = keep;
            }
        }
        return out;
    }

    private static byte[] encode(BufferedImage image) {
        try {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgb.createGraphics().drawImage(image, 0, 0, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            return ImageIO.write(rgb, "jpg", out) ? out.toByteArray() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
