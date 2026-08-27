package fileidea.rack.pricing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.BatchStatus;
import fileidea.rack.common.CompSource;
import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.common.Vendor;
import fileidea.rack.intake.Batch;
import fileidea.rack.intake.BatchRepository;
import fileidea.rack.intake.Item;
import fileidea.rack.intake.ItemRepository;
import fileidea.rack.integration.serpapi.SerpApiClient;
import fileidea.rack.integration.serpapi.SerpApiClient.EbayComp;
import fileidea.rack.integration.serpapi.SerpApiClient.ShoppingResult;
import fileidea.rack.task.TaskEndpoints;
import fileidea.rack.task.TaskOrchestrator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PricingService {

    /** Comps persisted per item as on-page evidence; the median still uses all results. */
    private static final int MAX_STORED_COMPS = 12;

    private static final Logger log = LoggerFactory.getLogger(PricingService.class);

    private final ItemRepository items;
    private final BatchRepository batches;
    private final PriceEstimateRepository estimates;
    private final CompRepository comps;
    private final SerpApiClient serpApi;
    private final PriceCalculator calculator;
    private final TaskOrchestrator tasks;

    public PricingService(
            ItemRepository items,
            BatchRepository batches,
            PriceEstimateRepository estimates,
            CompRepository comps,
            SerpApiClient serpApi,
            PriceCalculator calculator,
            TaskOrchestrator tasks
    ) {
        this.items = items;
        this.batches = batches;
        this.estimates = estimates;
        this.comps = comps;
        this.serpApi = serpApi;
        this.calculator = calculator;
        this.tasks = tasks;
    }

    @Transactional
    public PriceEstimate price(Long itemId) {
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("item " + itemId));
        String query = searchQuery(item);

        if (query.isBlank()) {
            // Identification produced nothing, so there is no question to ask eBay. Bailing here
            // rather than searching for an empty string saves three SerpApi calls per item, which
            // matters on a metered plan, and the outcome is identical: no comps, no listing.
            log.warn("item {} has no brand or type to search on — skipping pricing entirely", itemId);
            item.setStatus(ItemStatus.FAILED);
            items.save(item);
            Batch unidentified = item.getBatch();
            if (unidentified != null) {
                unidentified.setStatus(BatchStatus.PARTIAL_FAILURE);
                batches.save(unidentified);
            }
            return estimates.findByItemId(itemId).orElseGet(() -> {
                PriceEstimate empty = new PriceEstimate();
                empty.setItem(item);
                empty.setCurrency("USD");
                return estimates.save(empty);
            });
        }

        List<EbayComp> comparables = serpApi.ebayComps(query);
        List<ShoppingResult> shopping = serpApi.shopping(query);
        String brand = item.displayBrand() == null ? query : item.displayBrand();
        List<Integer> trends = serpApi.trendSeries(brand);

        List<BigDecimal> compPrices = new ArrayList<>();
        for (EbayComp hit : comparables) {
            if (hit.price() != null) {
                compPrices.add(hit.price());
            }
        }

        PriceCalculator.PriceSnapshot snap = calculator.compute(compPrices);
        double slope = calculator.slope(trends);

        PriceEstimate estimate = estimates.findByItemId(itemId).orElseGet(PriceEstimate::new);
        estimate.setItem(item);
        estimate.setMedianSoldPrice(snap.median());
        estimate.setP25(snap.p25());
        estimate.setP75(snap.p75());
        estimate.setRetailAnchor(pickRetail(shopping));
        estimate.setDemandDirection(calculator.demand(slope));
        estimate.setCompCount(snap.n());
        estimate.setCurrency("USD");
        estimate = estimates.save(estimate);

        comps.deleteAll(comps.findByPriceEstimateId(estimate.getId()));
        // The median above used every result, but only a sample is persisted as visible evidence
        // (eBay URLs run ~400 characters each, so keeping all 60 made the listing page 47KB of
        // links). Which sample matters: taking the first N produced pages that claimed
        // "comparables run $3.90-$31.98" while displaying comps of $35,011 - the evidence
        // contradicted the number it was supposed to support. Keeping the listings closest to the
        // median means what a reader sees is genuinely what the price was derived from.
        BigDecimal median = snap.median();
        List<EbayComp> evidence = comparables.stream()
                .filter(hit -> hit.price() != null && hit.sourceUrl() != null)
                .sorted(Comparator.comparing(hit -> hit.price().subtract(median).abs()))
                .limit(MAX_STORED_COMPS)
                .sorted(Comparator.comparing(EbayComp::price))
                .toList();

        for (EbayComp hit : evidence) {
            Comp row = new Comp();
            row.setPriceEstimate(estimate);
            row.setSource(CompSource.EBAY);
            row.setTitle(hit.title() == null ? "eBay sold listing" : hit.title());
            row.setPrice(hit.price());
            row.setSoldDate(hit.soldDate());
            row.setSourceUrl(hit.sourceUrl());
            comps.save(row);
        }

        Batch batch = item.getBatch();

        if (snap.median() == null) {
            // No sold comps means no defensible number. Rather than invent one, the item stops
            // here: it is not photographed (which would spend Perfect Corp credits on something
            // that can never be listed) and the batch is marked as a partial failure. The other
            // items in the batch are unaffected \u2014 each item is its own task chain.
            log.warn("no comparable listings for item {} (query: \"{}\") \u2014 failing it rather than inventing a price",
                    itemId, query);
            item.setStatus(ItemStatus.FAILED);
            items.save(item);
            refreshBatchTotal(batch);
            if (batch != null) {
                batch.setStatus(BatchStatus.PARTIAL_FAILURE);
                batches.save(batch);
            }
            return estimate;
        }

        item.setStatus(ItemStatus.PRICED);
        items.save(item);
        refreshBatchTotal(batch);
        if (batch != null) {
            batch.setStatus(BatchStatus.IMAGING);
            batches.save(batch);
        }
        tasks.enqueue(item, Vendor.PERFECT_CORP, TaskEndpoints.PHOTOGRAPH, TaskEndpoints.photographKey(item.getId()));
        return estimate;
    }

    @Transactional(readOnly = true)
    public PriceEstimate get(Long itemId) {
        return estimates.findByItemId(itemId)
                .orElseThrow(() -> new NotFoundException("price for item " + itemId));
    }

    @Transactional(readOnly = true)
    public List<Comp> compsFor(Long estimateId) {
        return comps.findByPriceEstimateId(estimateId);
    }


    /**
     * Joins only the pieces we actually have. Concatenating blanks produced queries like " "
     * or "Jeans ", which quietly returned junk comps instead of failing loudly.
     */
    public String searchQuery(Item item) {
        if (item == null) {
            return "";
        }
        List<String> pieces = new ArrayList<>();
        addIfPresent(pieces, item.displayBrand());
        addIfPresent(pieces, item.getIdentifiedType());
        return String.join(" ", pieces);
    }

    private static void addIfPresent(List<String> pieces, String value) {
        if (value != null && !value.isBlank()) {
            pieces.add(value.trim());
        }
    }


    /**
     * The top shopping hit is not guaranteed to carry a price, so scan for the first that does
     * rather than reading index 0 and publishing a null retail anchor.
     */
    public BigDecimal pickRetail(List<ShoppingResult> shopping) {
        if (shopping == null || shopping.isEmpty()) {
            return null;
        }
        for (ShoppingResult hit : shopping) {
            if (hit != null && hit.retailAnchor() != null) {
                return hit.retailAnchor();
            }
        }
        return null;
    }

    private void refreshBatchTotal(Batch batch) {
        if (batch == null || batch.getId() == null) {
            return;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Item item : items.findByBatchId(batch.getId())) {
            var est = estimates.findByItemId(item.getId());
            if (est.isPresent() && est.get().getMedianSoldPrice() != null) {
                sum = sum.add(est.get().getMedianSoldPrice());
            }
        }
        batch.setTotalEstimatedValue(sum);
        batches.save(batch);
    }
}
