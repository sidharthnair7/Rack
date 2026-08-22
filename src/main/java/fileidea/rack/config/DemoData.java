package fileidea.rack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import fileidea.rack.seller.Seller;
import fileidea.rack.seller.SellerRepository;
import fileidea.rack.store.Store;
import fileidea.rack.store.StoreRepository;
import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(prefix = "rack.demo", name = "seed", havingValue = "true", matchIfMissing = true)
public class DemoData {

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private final SellerRepository sellers;
    private final StoreRepository stores;

    private Long sellerId;
    private Long storeId;

    public DemoData(SellerRepository sellers, StoreRepository stores) {
        this.sellers = sellers;
        this.stores = stores;
    }

    @PostConstruct
    void seed() {
        Seller seller = sellers.findByEmail("demo@rack.local").orElseGet(() -> {
            Seller created = new Seller();
            created.setEmail("demo@rack.local");
            created.setPasswordHash("{noop}demo");
            return sellers.save(created);
        });
        Store store = stores.findBySellerId(seller.getId()).stream().findFirst().orElseGet(() -> {
            Store created = new Store();
            created.setSeller(seller);
            created.setName("Demo Closet");
            return stores.save(created);
        });
        this.sellerId = seller.getId();
        this.storeId = store.getId();
        log.info("demo sellerId={} storeId={}", sellerId, storeId);
    }

    public Long sellerId() {
        return sellerId;
    }

    public Long storeId() {
        return storeId;
    }
}
