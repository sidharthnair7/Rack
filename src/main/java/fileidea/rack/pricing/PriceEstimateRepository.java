package fileidea.rack.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceEstimateRepository extends JpaRepository<PriceEstimate, Long> {

    Optional<PriceEstimate> findByItemId(Long itemId);
}
