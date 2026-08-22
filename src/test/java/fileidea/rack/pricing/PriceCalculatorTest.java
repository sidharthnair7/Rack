package fileidea.rack.pricing;

import fileidea.rack.common.DemandDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void emptyListIsHonest() {
        var snap = calculator.compute(List.of());
        assertEquals(0, snap.n());
        assertNull(snap.median());
        assertNull(snap.p25());
        assertNull(snap.p75());
    }

    @Test
    void oddListMedianIsTheMiddle() {
        var snap = calculator.compute(prices(10, 20, 30, 40, 50));
        assertEquals(0, new BigDecimal("30.00").compareTo(snap.median()));
        assertEquals(5, snap.n());
        assertNotNull(snap.p25());
        assertNotNull(snap.p75());
        assertTrue(snap.p25().compareTo(snap.median()) <= 0);
        assertTrue(snap.p75().compareTo(snap.median()) >= 0);
    }

    @Test
    void outliersMustNotActLikeAMean() {
        var snap = calculator.compute(prices(10, 11, 12, 1000));
        assertTrue(snap.median().compareTo(new BigDecimal("20")) < 0,
                "median got pulled toward the outlier — use median, not mean");
    }

    @Test
    void flatSeriesIsFlatDemand() {
        assertEquals(DemandDirection.FLAT, calculator.demand(calculator.slope(List.of(50, 50, 50, 50, 50))));
    }

    @Test
    void risingSeriesIsRisingDemand() {
        assertEquals(DemandDirection.RISING, calculator.demand(calculator.slope(List.of(20, 30, 45, 60, 80))));
    }

    @Test
    void fallingSeriesIsFallingDemand() {
        assertEquals(DemandDirection.FALLING, calculator.demand(calculator.slope(List.of(80, 60, 45, 30, 20))));
    }

    private static List<BigDecimal> prices(int... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }
}
