package fileidea.rack.web.dto;

import fileidea.rack.imaging.ImageAsset;
import fileidea.rack.intake.Batch;
import fileidea.rack.intake.Item;
import fileidea.rack.listing.Listing;
import fileidea.rack.pricing.Comp;
import fileidea.rack.pricing.PriceEstimate;
import fileidea.rack.seller.Seller;
import fileidea.rack.store.Store;

import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public static SellerResponse seller(Seller seller) {
        return new SellerResponse(seller.getId(), seller.getEmail());
    }

    public static StoreResponse store(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getSeller().getId(),
                store.getName(),
                store.getDomain()
        );
    }

    public static ItemResponse item(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getBatch().getId(),
                item.getSourceImageUrl(),
                item.getIdentifiedBrand(),
                item.getIdentifiedType(),
                item.getCategory(),
                item.getCondition(),
                item.getUserCorrectedBrand(),
                item.displayBrand(),
                item.getStatus()
        );
    }

    public static BatchResponse batch(Batch batch, List<Item> items) {
        return new BatchResponse(
                batch.getId(),
                batch.getStore().getId(),
                batch.getUploadedAt(),
                batch.getStatus(),
                batch.getItemCount(),
                batch.getTotalEstimatedValue(),
                items.stream().map(Dtos::item).toList()
        );
    }

    public static ImageResponse image(ImageAsset asset) {
        return new ImageResponse(asset.getKind(), asset.getUrl(), asset.getStatus());
    }

    public static ListingResponse listing(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getItem().getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getAskingPrice(),
                listing.getPublishedAt()
        );
    }

    public static CompResponse comp(Comp comp) {
        return new CompResponse(
                comp.getSource(),
                comp.getTitle(),
                comp.getPrice(),
                comp.getSoldDate(),
                comp.getSourceUrl()
        );
    }

    public static PricePanelResponse pricePanel(Item item, PriceEstimate estimate, List<Comp> comps) {
        int n = estimate.getMedianSoldPrice() == null ? 0 : comps.size();
        String warning = n > 0 && n < 4
                ? "Limited comp data (" + n + " sales found) — treat as an estimate."
                : null;
        String brand = item.displayBrand() == null ? "Unknown" : item.displayBrand();
        String type = item.getIdentifiedType() == null ? "" : item.getIdentifiedType();
        return new PricePanelResponse(
                item.getId(),
                (brand + " — " + type).trim(),
                item.getCondition(),
                estimate.getMedianSoldPrice(),
                estimate.getP25(),
                estimate.getP75(),
                n,
                estimate.getRetailAnchor(),
                estimate.getDemandDirection(),
                warning,
                comps.stream().map(Dtos::comp).toList()
        );
    }
}
