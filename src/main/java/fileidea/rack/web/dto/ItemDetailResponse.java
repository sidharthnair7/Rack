package fileidea.rack.web.dto;

import java.util.List;

public record ItemDetailResponse(
        ItemResponse item,
        PricePanelResponse price,
        List<ImageResponse> images,
        ListingResponse listing
) {
}
