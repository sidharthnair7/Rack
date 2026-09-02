package fileidea.rack.intake;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Splitting one photo into one item per garment is the difference between "photograph a piece" and
 * "photograph the pile", so the region finder is tested directly rather than through the vendor.
 *
 * <p>The input it operates on is the alpha channel of Perfect Corp's background removal: opaque
 * where a garment is, transparent everywhere else. These fixtures build that mask by hand.
 */
class GarmentDetectionTest {

    /** Transparent canvas with opaque rectangles painted on, exactly like a real cutout. */
    private static BufferedImage mask(int w, int h, Rectangle... blobs) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (Rectangle b : blobs) {
            for (int y = b.y; y < b.y + b.height; y++) {
                for (int x = b.x; x < b.x + b.width; x++) {
                    img.setRGB(x, y, 0xFF000000);
                }
            }
        }
        return img;
    }

    @Test
    void twoSeparateGarmentsBecomeTwoRegions() {
        BufferedImage m = mask(400, 400,
                new Rectangle(20, 20, 140, 300),
                new Rectangle(240, 30, 130, 280));
        List<Rectangle> regions = GarmentDetector.regions(m, 400, 400);
        assertEquals(2, regions.size());
    }

    @Test
    void regionsComeBackInOriginalPhotoCoordinates() {
        // Mask is 400 wide, the photo it came from is 4000 wide: boxes must scale up 10x.
        BufferedImage m = mask(400, 400,
                new Rectangle(20, 20, 140, 300),
                new Rectangle(240, 30, 130, 280));
        List<Rectangle> regions = GarmentDetector.regions(m, 4000, 4000);
        assertEquals(2, regions.size());
        for (Rectangle r : regions) {
            assertTrue(r.width > 800, "a garment filling a third of the frame should be ~1000px wide, got " + r.width);
            assertTrue(r.x >= 0 && r.y >= 0, "a padded box must not start outside the photo");
            assertTrue(r.x + r.width <= 4000 && r.y + r.height <= 4000, "a padded box must not run past the edge");
        }
    }

    /** The single-garment path is the one that already worked, and it must not change. */
    @Test
    void oneGarmentIsNotSplit() {
        BufferedImage m = mask(400, 400, new Rectangle(80, 40, 240, 320));
        assertTrue(GarmentDetector.regions(m, 400, 400).isEmpty(),
                "fewer than two regions means the caller keeps the original photo untouched");
    }

    @Test
    void specksAndShadowsAreNotGarments() {
        BufferedImage m = mask(400, 400,
                new Rectangle(20, 20, 150, 300),   // a real garment
                new Rectangle(250, 40, 140, 280),  // another real garment
                new Rectangle(200, 380, 4, 4),     // a speck
                new Rectangle(10, 390, 6, 3));     // another speck
        assertEquals(2, GarmentDetector.regions(m, 400, 400).size());
    }

    /** A rail of clothing is not a pile; better to treat the frame as one thing than shred it. */
    @Test
    void tooManyRegionsFallsBackToTheWholeFrame() {
        Rectangle[] many = new Rectangle[8];
        for (int i = 0; i < many.length; i++) {
            many[i] = new Rectangle(10 + i * 48, 100, 40, 200);
        }
        assertTrue(GarmentDetector.regions(mask(400, 400, many), 400, 400).isEmpty());
    }

    /**
     * The guard clause exists so a bad input degrades instead of failing the upload. It has to
     * actually survive one: {@code List.of} rejects null, so the line meant to be the safety net
     * was the line that would have thrown.
     */
    @Test
    void aNullOrEmptyPhotoDoesNotThrow() {
        GarmentDetector detector = new GarmentDetector(null,
                new fileidea.rack.config.PerfectCorpProperties("key", "https://example.com", null));
        assertEquals(List.of(), detector.split(null));
        assertEquals(List.of(), detector.split(new byte[0]));
    }

    @Test
    void withoutAVendorKeyThePhotoIsReturnedUnchanged() {
        GarmentDetector detector = new GarmentDetector(null,
                new fileidea.rack.config.PerfectCorpProperties(null, "https://example.com", null));
        byte[] photo = {1, 2, 3};
        assertEquals(1, detector.split(photo).size());
        assertEquals(photo, detector.split(photo).get(0));
    }

    @Test
    void anEmptyOrOpaqueMaskYieldsNothingToSplit() {
        assertTrue(GarmentDetector.regions(mask(400, 400), 400, 400).isEmpty(), "nothing in frame");
        // A JPEG has no alpha, so there is no foreground information to read.
        assertTrue(GarmentDetector.regions(
                new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB), 400, 400).isEmpty());
        assertTrue(GarmentDetector.regions(null, 400, 400).isEmpty());
    }

    /**
     * People photograph piles by dropping clothes on a bed, so garments touch. Two pieces joined
     * by a narrow bridge - a sleeve resting on a hem - are one shape to plain labelling. Shaving
     * the mask boundary breaks that bridge while the bodies survive.
     */
    @Test
    void garmentsTouchingAlongANarrowBridgeAreStillSeparated() {
        BufferedImage m = mask(400, 400,
                new Rectangle(30, 40, 150, 300),   // one garment
                new Rectangle(220, 40, 150, 300),  // another
                new Rectangle(180, 180, 40, 6));   // a sleeve lying across the gap
        assertEquals(2, GarmentDetector.regions(m, 400, 400).size(),
                "erosion should break the bridge and recover both garments");
    }

    /**
     * The guard that makes erosion safe: it is only ever attempted when plain labelling found
     * nothing to split, so it can add detections but never break a correct single-garment answer.
     * A shirt is narrow at the waist and thin at the straps, and neither may shatter it.
     */
    @Test
    void erosionNeverShattersASingleGarment() {
        // A waisted shape: wide shoulders, narrow middle, wide hem.
        BufferedImage waisted = mask(400, 400,
                new Rectangle(120, 40, 160, 110),
                new Rectangle(178, 150, 44, 90),
                new Rectangle(120, 240, 160, 120));
        assertTrue(GarmentDetector.regions(waisted, 400, 400).isEmpty(),
                "one garment with a narrow waist must stay one item");
    }

    /** Heavily overlapping pieces still read as one. Honest limit, pinned so it stays known. */
    @Test
    void garmentsPiledDirectlyOnTopOfEachOtherRemainOneRegion() {
        BufferedImage m = mask(400, 400,
                new Rectangle(20, 20, 200, 300),
                new Rectangle(150, 30, 200, 280));
        assertTrue(GarmentDetector.regions(m, 400, 400).isEmpty(),
                "a deep overlap is genuinely one shape; spreading the pile out is the fix");
    }
}
