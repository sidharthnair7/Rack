package fileidea.rack.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.imaging.ImageAssetRepository;
import fileidea.rack.listing.ListingRepository;
import fileidea.rack.pricing.PriceEstimate;
import fileidea.rack.pricing.PricingService;
import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.ItemDetailResponse;
import fileidea.rack.web.dto.PricePanelResponse;

@Service
public class ItemDetailService {

    private final IntakeService intake;
    private final PricingService pricing;
    private final ImageAssetRepository images;
    private final ListingRepository listings;

    public ItemDetailService(
            IntakeService intake,
            PricingService pricing,
            ImageAssetRepository images,
            ListingRepository listings
    ) {
        this.intake = intake;
        this.pricing = pricing;
        this.images = images;
        this.listings = listings;
    }

    @Transactional(readOnly = true)
    public ItemDetailResponse detail(Long itemId) {
        var item = intake.getItem(itemId);
        PricePanelResponse price = null;
        try {
            PriceEstimate estimate = pricing.get(itemId);
            price = Dtos.pricePanel(item, estimate, pricing.compsFor(estimate.getId()));
        } catch (Exception ignored) {
        }
        var listing = listings.findByItemId(itemId).map(Dtos::listing).orElse(null);
        return new ItemDetailResponse(
                Dtos.item(item),
                price,
                images.findByItemId(itemId).stream().map(Dtos::image).toList(),
                listing
        );
    }
}
