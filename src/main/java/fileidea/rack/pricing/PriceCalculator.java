package fileidea.rack.pricing;

import org.springframework.stereotype.Component;

import fileidea.rack.common.DemandDirection;
import fileidea.rack.common.NotImplemented;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic. Never an LLM. Suggested = median of sold comps. Range = p25–p75.
 *
 * YOU write the three methods below. Spec and examples live in PriceCalculatorTest.
 */
@Component
public class PriceCalculator {

    /**
     * YOUR LOGIC.
     *
     * @param soldPrices sold-comp prices, any order, may be empty
     * @return median / 25th percentile / 75th percentile / count.
     *         Empty input → all money fields null, n = 0.
     *         Do not use a mean — outliers will wreck it.
     */
    public PriceSnapshot compute(List<BigDecimal> soldPrices) {
        return NotImplemented.yet("YOUR LOGIC: PriceCalculator.compute (median + p25/p75)");
    }

    /**
     * YOUR LOGIC.
     *
     * Turn a 12-month Google Trends series into a single slope number.
     * You pick the formula (first-vs-last, regression, whatever) — just be consistent.
     */
    public double slope(List<Integer> trendSeries) {
        return NotImplemented.yet("YOUR LOGIC: PriceCalculator.slope");
    }

    /**
     * YOUR LOGIC.
     *
     * Map slope → RISING / FLAT / FALLING. Pick your own thresholds.
     */
    public DemandDirection demand(double slope) {
        return NotImplemented.yet("YOUR LOGIC: PriceCalculator.demand");
    }

    public record PriceSnapshot(BigDecimal median, BigDecimal p25, BigDecimal p75, int n) {
    }
}
