package fileidea.rack.pricing;

import fileidea.rack.common.DemandDirection;
import fileidea.rack.integration.serpapi.SerpApiClient.ShoppingResult;
import fileidea.rack.intake.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the seams where a missing value used to leak through as a blank query or a null
 * retail anchor instead of being handled.
 */
class PricingQueryTest {

    private final PricingService pricing = new PricingService(null, null, null, null, null, null, null, null);
    private final PriceCalculator calculator = new PriceCalculator();

    private static Item item(String brand, String type) {
        Item item = new Item();
        item.setIdentifiedBrand(brand);
        item.setIdentifiedType(type);
        return item;
    }

    @Test
    void joinsOnlyThePiecesThatExist() {
        assertEquals("Levi's Jeans", pricing.searchQuery(item("Levi's", "Jeans")));
        assertEquals("Levi's", pricing.searchQuery(item("Levi's", null)));
        assertEquals("Jeans", pricing.searchQuery(item(null, "Jeans")));
    }

    @Test
    void anUnidentifiedItemProducesAnEmptyQueryNotAStraySpace() {
        String query = pricing.searchQuery(item(null, null));
        assertEquals("", query);
        assertFalse(query.contains(" "), "a blank-joined query searched eBay for nothing in particular");
    }

    @Test
    void aCorrectedBrandOverridesTheGuess() {
        Item corrected = item("Levl's", "Jeans");
        corrected.setUserCorrectedBrand("Levi's");
        assertEquals("Levi's Jeans", pricing.searchQuery(corrected));
    }

    @Test
    void retailAnchorSkipsPricelessResults() {
        List<ShoppingResult> hits = Arrays.asList(
                new ShoppingResult(null, "No price on this one", "https://example.com/a"),
                new ShoppingResult(new BigDecimal("98.00"), "Levi's 501", "https://example.com/b")
        );
        assertEquals(0, new BigDecimal("98.00").compareTo(pricing.pickRetail(hits)));
    }

    @Test
    void retailAnchorIsNullWhenNothingHasAPrice() {
        assertNull(pricing.pickRetail(List.of()));
        assertNull(pricing.pickRetail(Arrays.asList(
                new ShoppingResult(null, "a", "https://example.com/a")
        )));
    }

    @Test
    void evenNumberedCompsUseATrueMedian() {
        var snap = calculator.compute(List.of(
                new BigDecimal("40"), new BigDecimal("50"),
                new BigDecimal("60"), new BigDecimal("70")
        ));
        assertEquals(0, new BigDecimal("55.00").compareTo(snap.median()),
                "the median of four values is the mean of the middle two, not the upper one");
    }

    @Test
    void nullPricesAreDroppedRatherThanCrashingTheSort() {
        var snap = calculator.compute(Arrays.asList(new BigDecimal("30"), null, new BigDecimal("50")));
        assertNotNull(snap.median());
        assertEquals(2, snap.n());
    }

    @Test
    void aTinyWobbleIsNotADemandTrend() {
        assertEquals(DemandDirection.FLAT, calculator.demand(calculator.slope(List.of(50, 52, 49, 51, 51))),
                "one point of movement on a 0-100 index is noise, and the panel would claim 'Rising'");
    }

    @Test
    void aRealClimbStillReadsAsRising() {
        assertEquals(DemandDirection.RISING, calculator.demand(calculator.slope(List.of(20, 35, 50, 65, 80))));
    }
}
