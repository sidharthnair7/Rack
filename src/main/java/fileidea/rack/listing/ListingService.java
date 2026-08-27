package fileidea.rack.listing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.BatchStatus;
import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.intake.Batch;
import fileidea.rack.intake.BatchRepository;
import fileidea.rack.intake.Item;
import fileidea.rack.intake.ItemRepository;
import fileidea.rack.integration.llm.CopyGenerator;
import fileidea.rack.integration.stripe.StripeClient;
import fileidea.rack.pricing.PriceEstimate;
import fileidea.rack.pricing.PriceEstimateRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ListingService {

    private final ItemRepository items;
    private final BatchRepository batches;
    private final ListingRepository listings;
    private final PriceEstimateRepository estimates;
    private final CopyGenerator copy;
    private final StripeClient stripe;

    public ListingService(
            ItemRepository items,
            BatchRepository batches,
            ListingRepository listings,
            PriceEstimateRepository estimates,
            CopyGenerator copy,
            StripeClient stripe
    ) {
        this.items = items;
        this.batches = batches;
        this.listings = listings;
        this.estimates = estimates;
        this.copy = copy;
        this.stripe = stripe;
    }

    @Transactional
    public Listing publish(Long itemId) {
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("item " + itemId));
        PriceEstimate estimate = estimates.findByItemId(itemId)
                .orElseThrow(() -> new NotFoundException("price for item " + itemId + " — cannot publish without a sourced price"));
        BigDecimal asking = estimate.getMedianSoldPrice();
        if (asking == null) {
            throw new IllegalStateException("no sourced price on item " + itemId);
        }

        Listing listing = listings.findByItemId(itemId).orElseGet(Listing::new);
        BigDecimal previousPrice = listing.getAskingPrice();
        listing.setItem(item);
        listing.setTitle(copy.listingTitle(item.displayBrand(), item.getIdentifiedType(), item.getCategory()));
        listing.setDescription(copy.listingDescription(
                item.displayBrand(),
                item.getIdentifiedType(),
                item.getCategory(),
                item.getCondition()
        ));
        listing.setAskingPrice(asking);
        listing.setPublishedAt(Instant.now());

        // Re-issue the checkout whenever the price changes - a brand correction re-runs publish -
        // so a buyer can never reach a link that still charges yesterday's number.
        boolean repriced = previousPrice == null || previousPrice.compareTo(asking) != 0;
        if (repriced || listing.getCheckoutUrl() == null) {
            listing.setCheckoutUrl(stripe.createCheckoutLink(
                    listing.getTitle(), listing.getDescription(), asking));
        }
        listings.save(listing);

        item.setStatus(ItemStatus.LISTED);
        items.save(item);
        reconcile(item.getBatch());
        return listing;
    }

    @Transactional(readOnly = true)
    public Listing get(Long itemId) {
        return listings.findByItemId(itemId).orElseThrow(() -> new NotFoundException("listing for item " + itemId));
    }

    @Transactional(readOnly = true)
    public List<Listing> forStore(Long storeId) {
        return batches.findByStoreId(storeId).stream()
                .flatMap(batch -> items.findByBatchId(batch.getId()).stream())
                .filter(item -> item.getStatus() == ItemStatus.LISTED)
                .map(item -> listings.findByItemId(item.getId()).orElse(null))
                .filter(listing -> listing != null)
                .toList();
    }

    private void reconcile(Batch batch) {
        if (batch == null || batch.getId() == null) {
            return;
        }
        List<Item> all = items.findByBatchId(batch.getId());
        long listed = all.stream().filter(i -> i.getStatus() == ItemStatus.LISTED).count();
        long failed = all.stream().filter(i -> i.getStatus() == ItemStatus.FAILED).count();
        if (listed == all.size()) {
            batch.setStatus(BatchStatus.COMPLETE);
        } else if (listed + failed == all.size()) {
            // Everything has settled, some of it badly. A partly-listed batch is a real outcome,
            // not a stuck one, and the storefront still shows the items that priced cleanly.
            batch.setStatus(BatchStatus.PARTIAL_FAILURE);
        } else {
            batch.setStatus(BatchStatus.PUBLISHING);
        }
        batches.save(batch);
    }
}
