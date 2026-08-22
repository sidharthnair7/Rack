package fileidea.rack.web.dto;

import fileidea.rack.common.BatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BatchResponse(
        Long id,
        Long storeId,
        Instant uploadedAt,
        BatchStatus status,
        int itemCount,
        BigDecimal totalEstimatedValue,
        List<ItemResponse> items
) {
}
