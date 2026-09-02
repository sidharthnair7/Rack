package fileidea.rack.identify;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The brand is the first thing the pipeline decides and everything downstream inherits it: it
 * becomes the eBay query, so it determines the comps, the median and the listing title. A wrong
 * brand is not a cosmetic label problem, it is a wrong price.
 */
class BrandExtractionTest {

    /**
     * The regression this was written for. A real pair of jeans came back branded "Solid",
     * because colour and fit words recur across independent listings exactly the way a brand
     * does. Frequency alone cannot separate them; the brand is the word that leads the title.
     */
    @Test
    void aDescriptorThatRecursDoesNotBeatTheBrandThatLeads() {
        List<String> titles = List.of(
                "Levi's Men's Slim Straight Jeans Blue Medium Wash Denim Cotton Mid Rise",
                "Levi's Men's 500 Series Blue Medium Wash Denim Jeans Solid Logo Accent",
                "Copper Rivet Men's Slim Fit Distressed Blue Denim Jeans Solid 5-Pkt",
                "Blue Drawstring Jeans Solid Wash"
        );
        assertEquals("Levi's", IdentifyService.recurringBrand(titles));
    }

    @Test
    void aBrandStillWinsWhenItIsNotTheMostFrequentWord() {
        // "Outfitters" trails the brand in every title, so frequency alone would tie or lose.
        // The recognised name also keeps it whole instead of truncating to "American".
        List<String> titles = List.of(
                "American Eagle Outfitters Blue Dark Wash Jeans",
                "American Eagle Outfitters Cotton Hooded Sweatshirt",
                "Urban Outfitters Cropped Denim Jacket"
        );
        assertEquals("American Eagle", IdentifyService.recurringBrand(titles));
    }

    /** A word appearing in only one title is noise, not a brand, and must not be promoted. */
    @Test
    void aSingleAppearanceIsNotEnoughToBeABrand() {
        List<String> titles = List.of(
                "Patagonia Better Sweater Fleece Jacket",
                "Arcteryx Beta AR Shell",
                "Snow Peak Insulated Parka"
        );
        // Nothing recurs, so it falls back to the first surviving token rather than inventing one.
        assertEquals("Patagonia", IdentifyService.recurringBrand(titles));
    }

    @Test
    void stockPhotoBoilerplateNeverBecomesTheBrand() {
        List<String> titles = List.of(
                "Stock Photo Isolated Denim Jacket White Background",
                "Shutterstock Royalty Free Image Denim Jacket Flat Lay",
                "Stock Image Denim Jacket Closeup Isolated"
        );
        assertNull(IdentifyService.recurringBrand(titles),
                "every token is boilerplate or a garment type, so there is no brand to report");
    }

    @Test
    void emptyOrMissingInputIsNotAnError() {
        assertNull(IdentifyService.recurringBrand(null));
        assertNull(IdentifyService.recurringBrand(List.of()));
    }

    /**
     * A recognised name outranks the frequency heuristic entirely. This is the case the heuristic
     * kept getting wrong: block "Solid" and it answered "Long", because no amount of word counting
     * separates an adjective from a brand.
     */
    @Test
    void aRecognisedBrandBeatsAnyDescriptor() {
        List<String> titles = List.of(
                "Long Sleeve Solid Wash Denim Jeans Levi's Mid Rise",
                "Levi's 501 Long Solid Blue Jeans",
                "Long Solid Straight Leg Jeans Levi's"
        );
        assertEquals("Levi's", IdentifyService.recurringBrand(titles));
    }

    @Test
    void aMultiWordBrandIsNotShortenedToTheWordInsideIt() {
        List<String> titles = List.of(
                "The North Face Nuptse Puffer Jacket",
                "The North Face 700 Down Jacket Black"
        );
        assertEquals("The North Face", IdentifyService.recurringBrand(titles));

        List<String> eagle = List.of(
                "American Eagle Slim Jeans Dark Wash",
                "American Eagle Denim Jeans Mens 32x30"
        );
        assertEquals("American Eagle", IdentifyService.recurringBrand(eagle));
    }

    /** One mention can be a "similar to" suggestion, so it is not enough to claim the brand. */
    @Test
    void aSingleBrandMentionDoesNotWin() {
        List<String> titles = List.of(
                "Unbranded Cotton Hooded Sweatshirt similar to Nike",
                "Plain Grey Hoodie Cotton",
                "Basic Pullover Hoodie Grey"
        );
        assertEquals(null, IdentifyService.knownBrandIn(titles));
    }
}
