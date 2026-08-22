package fileidea.rack.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findBySellerId(Long sellerId);
}
