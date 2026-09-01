package fileidea.rack.pricing;

import fileidea.rack.integration.serpapi.SerpApiClient.EbayComp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * eBay returns one item more than once for plenty of queries: relists, promoted placements and
 * variation groupings all arrive as separate rows pointing at the same listing. Both the median
 * and the on-page evidence are computed from this list, so a duplicate skews the price and shows
 * the same garment twice in the panel that exists to prove the price is real.
 */
class CompDeduplicationTest {

    private static EbayComp comp(String title, String price, String url) {
        return new EbayComp(title, price == null ? null : new BigDecimal(price), null, url);
    }

    @Test
    void collapsesTheSameListingReturnedTwice() {
        List<EbayComp> distinct = PricingService.distinctListings(List.of(
                comp("Carhartt Detroit Jacket", "88.00", "https://www.ebay.com/itm/1111"),
                comp("Carhartt Detroit Jacket", "88.00", "https://www.ebay.com/itm/1111"),
                comp("Carhartt Chore Coat", "94.50", "https://www.ebay.com/itm/2222")));

        assertEquals(2, distinct.size());
        assertEquals("https://www.ebay.com/itm/1111", distinct.get(0).sourceUrl());
    }

    @Test
    void ignoresPerImpressionTrackingParameters() {
        // The same item, linked twice by eBay with different tracking query strings. Compared as
        // raw strings these look like two separate listings.
        List<EbayComp> distinct = PricingService.distinctListings(List.of(
                comp("Levi's 501", "45.00", "https://www.ebay.com/itm/9988?hash=item5f2&var=0"),
                comp("Levi's 501", "45.00", "https://www.ebay.com/itm/9988?epid=2201&campid=53384")));

        assertEquals(1, distinct.size());
    }

    @Test
    void fallsBackToTitleAndPriceWhenThereIsNoUrl() {
        List<EbayComp> distinct = PricingService.distinctListings(List.of(
                comp("Nike Windbreaker", "30.00", null),
                comp("  nike windbreaker  ", "30.00", null),
                comp("Nike Windbreaker", "42.00", null)));

        assertEquals(2, distinct.size());
    }

    @Test
    void keepsGenuinelyDifferentListingsThatHappenToSharePrice() {
        // Two different garments at the same price are not duplicates, so identity is the listing
        // rather than the number.
        List<EbayComp> distinct = PricingService.distinctListings(List.of(
                comp("Patagonia Fleece", "60.00", "https://www.ebay.com/itm/3333"),
                comp("Patagonia Fleece", "60.00", "https://www.ebay.com/itm/4444")));

        assertEquals(2, distinct.size());
    }

    @Test
    void preservesOrderAndSurvivesEmptyInput() {
        assertTrue(PricingService.distinctListings(List.of()).isEmpty());
        assertTrue(PricingService.distinctListings(null).isEmpty());

        List<EbayComp> distinct = PricingService.distinctListings(List.of(
                comp("first", "10.00", "https://www.ebay.com/itm/1"),
                comp("second", "20.00", "https://www.ebay.com/itm/2"),
                comp("first again", "10.00", "https://www.ebay.com/itm/1")));

        assertEquals(2, distinct.size());
        assertEquals("first", distinct.get(0).title());
        assertEquals("second", distinct.get(1).title());
    }
}
