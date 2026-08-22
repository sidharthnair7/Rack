package fileidea.rack.web.dto;

import fileidea.rack.common.CompSource;

import java.math.BigDecimal;
import java.time.Instant;

public record CompResponse(
        CompSource source,
        String title,
        BigDecimal price,
        Instant soldDate,
        String sourceUrl
) {
}
