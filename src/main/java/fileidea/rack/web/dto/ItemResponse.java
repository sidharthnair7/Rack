package fileidea.rack.web.dto;

import fileidea.rack.common.ItemStatus;

public record ItemResponse(
        Long id,
        Long batchId,
        String sourceImageUrl,
        String identifiedBrand,
        String identifiedType,
        String category,
        String condition,
        String userCorrectedBrand,
        String displayBrand,
        ItemStatus status
) {
}
