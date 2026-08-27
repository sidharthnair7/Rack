package fileidea.rack.pricing;

import org.springframework.stereotype.Component;

import fileidea.rack.common.DemandDirection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;


@Component
public class PriceCalculator {


    private static final double DEMAND_DEADBAND = 0.08;

    public PriceSnapshot compute(List<BigDecimal> soldPrices) {
        if (soldPrices == null || soldPrices.isEmpty()) {
            return new PriceSnapshot(null, null, null, 0);
        }
        List<BigDecimal> sorted = soldPrices.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (sorted.isEmpty()) {
            return new PriceSnapshot(null, null, null, 0);
        }

        int n = sorted.size();
        return new PriceSnapshot(median(sorted), at(sorted, 0.25), at(sorted, 0.75), n);
    }


    private static BigDecimal median(List<BigDecimal> sorted) {
        int n = sorted.size();
        int mid = n / 2;
        if (n % 2 == 1) {
            return sorted.get(mid).setScale(2, RoundingMode.HALF_UP);
        }
        return sorted.get(mid - 1)
                .add(sorted.get(mid))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal at(List<BigDecimal> sorted, double percentile) {
        int n = sorted.size();
        int index = (int) Math.round(percentile * (n - 1));
        index = Math.max(0, Math.min(n - 1, index));
        return sorted.get(index).setScale(2, RoundingMode.HALF_UP);
    }


    public double slope(List<Integer> trendSeries) {
        if (trendSeries == null || trendSeries.size() < 2) {
            return 0;
        }
        List<Integer> points = trendSeries.stream().filter(java.util.Objects::nonNull).toList();
        if (points.size() < 2) {
            return 0;
        }
        double first = points.get(0);
        double last = points.get(points.size() - 1);
        double average = points.stream().mapToInt(Integer::intValue).average().orElse(0);
        if (average <= 0) {
            return 0;
        }
        return (last - first) / average;
    }

    public DemandDirection demand(double slope) {
        if (slope <= -DEMAND_DEADBAND) {
            return DemandDirection.FALLING;
        }
        if (slope >= DEMAND_DEADBAND) {
            return DemandDirection.RISING;
        }
        return DemandDirection.FLAT;
    }

    public record PriceSnapshot(BigDecimal median, BigDecimal p25, BigDecimal p75, int n) {
    }
}
