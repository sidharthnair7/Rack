package fileidea.rack.pricing;

import org.springframework.stereotype.Component;

import fileidea.rack.common.DemandDirection;
import fileidea.rack.common.NotImplemented;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class PriceCalculator {


    public PriceSnapshot compute(List<BigDecimal> soldPrices) {
        if (soldPrices == null || soldPrices.isEmpty()) {
            return new PriceSnapshot(null, null, null, 0);
        }
        List<BigDecimal> sortedPrices = soldPrices.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        BigDecimal medianPrice = sortedPrices.get(sortedPrices.size() / 2);
        int size= sortedPrices.size();
        BigDecimal p25= sortedPrices.get(size / 4);
        BigDecimal p75= sortedPrices.get((size*3)/4);

        return new PriceSnapshot(medianPrice,p25,p75,size);



    }


    public double slope(List<Integer> trendSeries) {
        if(trendSeries == null || trendSeries.isEmpty()) {
            return 0;
        }
        double slope = (trendSeries.get(trendSeries.size()-1))-trendSeries.get(0);
        return slope;
    }


    public DemandDirection demand(double slope) {
        if(slope < 0) {
            return DemandDirection.FALLING;
        }
        if(slope > 0) {
            return DemandDirection.RISING;
        }

        return DemandDirection.FLAT;

    }

    public record PriceSnapshot(BigDecimal median, BigDecimal p25, BigDecimal p75, int n) {
    }
}
