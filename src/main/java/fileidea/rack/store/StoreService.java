package fileidea.rack.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.NotFoundException;
import fileidea.rack.seller.Seller;
import fileidea.rack.seller.SellerRepository;

@Service
public class StoreService {

    private final StoreRepository stores;
    private final SellerRepository sellers;

    public StoreService(StoreRepository stores, SellerRepository sellers) {
        this.stores = stores;
        this.sellers = sellers;
    }

    @Transactional
    public Store create(Long sellerId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("store name is required");
        }
        Seller seller = sellers.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("seller " + sellerId));
        Store store = new Store();
        store.setSeller(seller);
        store.setName(name.trim());
        return stores.save(store);
    }

    @Transactional(readOnly = true)
    public Store get(Long id) {
        return stores.findById(id).orElseThrow(() -> new NotFoundException("store " + id));
    }
}
