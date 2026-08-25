package fileidea.rack.pricing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.BatchStatus;
import fileidea.rack.common.CompSource;
import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.common.NotImplemented;
import fileidea.rack.common.Vendor;
import fileidea.rack.intake.Batch;
import fileidea.rack.intake.BatchRepository;
import fileidea.rack.intake.Item;
import fileidea.rack.intake.ItemRepository;
import fileidea.rack.integration.serpapi.SerpApiClient;
import fileidea.rack.integration.serpapi.SerpApiClient.EbaySoldComp;
import fileidea.rack.integration.serpapi.SerpApiClient.ShoppingResult;
import fileidea.rack.task.TaskEndpoints;
import fileidea.rack.task.TaskOrchestrator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {

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

        List<EbaySoldComp> sold = serpApi.ebaySold(query);
        List<ShoppingResult> shopping = serpApi.shopping(query);
        String brand = item.displayBrand() == null ? query : item.displayBrand();
        List<Integer> trends = serpApi.trendSeries(brand);

        List<BigDecimal> soldPrices = new ArrayList<>();
        for (EbaySoldComp hit : sold) {
            soldPrices.add(hit.price());
        }

        PriceCalculator.PriceSnapshot snap = calculator.compute(soldPrices);
        double slope = calculator.slope(trends);

        PriceEstimate estimate = estimates.findByItemId(itemId).orElseGet(PriceEstimate::new);
        estimate.setItem(item);
        estimate.setMedianSoldPrice(snap.median());
        estimate.setP25(snap.p25());
        estimate.setP75(snap.p75());
        estimate.setRetailAnchor(pickRetail(shopping));
        estimate.setDemandDirection(calculator.demand(slope));
        estimate.setCurrency("USD");
        estimate = estimates.save(estimate);

        comps.deleteAll(comps.findByPriceEstimateId(estimate.getId()));
        for (EbaySoldComp hit : sold) {
            Comp row = new Comp();
            row.setPriceEstimate(estimate);
            row.setSource(CompSource.EBAY);
            row.setTitle(hit.title() == null ? "eBay sold listing" : hit.title());
            row.setPrice(hit.price());
            row.setSoldDate(hit.soldDate());
            row.setSourceUrl(hit.sourceUrl());
            comps.save(row);
        }

        item.setStatus(ItemStatus.PRICED);
        items.save(item);
        refreshBatchTotal(item.getBatch());
        Batch batch = item.getBatch();
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


    public String searchQuery(Item item) {
        if(item==null) {
            return "";
        }
        String itemBrand = item.displayBrand() == null ? "" : item.displayBrand();
        String itemType = item.getIdentifiedType()== null ? "" : item.getIdentifiedType();

        return itemBrand +" "+ itemType;
    }


    public BigDecimal pickRetail(List<ShoppingResult> shopping) {
        if(shopping==null || shopping.isEmpty()) {
            return null;
        }
        ShoppingResult firstItemPrice= shopping.getFirst();
        return firstItemPrice.retailAnchor();

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
