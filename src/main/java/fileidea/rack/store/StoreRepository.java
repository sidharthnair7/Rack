package fileidea.rack.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findBySellerId(Long sellerId);

    /**
     * Resolve the store a request's hostname belongs to.
     *
     * <p>This is what makes a registered domain more than a receipt. A visitor typing the seller's
     * own address has to land on the seller's shop rather than on Rack's marketing page, and that
     * lookup is the difference between "we called the registration API" and "you own a storefront".
     * Case-insensitive because hostnames are, and DNS hands us whatever case the visitor typed.
     */
    Optional<Store> findByDomainIgnoreCase(String domain);
}
