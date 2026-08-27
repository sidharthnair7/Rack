package fileidea.rack.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingResponse(
        Long id,
        Long itemId,
        String title,
        String description,
        BigDecimal askingPrice,
        Instant publishedAt,
        String checkoutUrl
) {
}
