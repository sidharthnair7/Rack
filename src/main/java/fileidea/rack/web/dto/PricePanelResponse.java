package fileidea.rack.web.dto;

import fileidea.rack.common.DemandDirection;

import java.math.BigDecimal;
import java.util.List;

public record PricePanelResponse(
        Long itemId,
        String heading,
        String condition,
        BigDecimal suggested,
        BigDecimal rangeLow,
        BigDecimal rangeHigh,
        int compCount,
        BigDecimal retailNew,
        DemandDirection demand,
        String warning,
        List<CompResponse> comps
) {
}
