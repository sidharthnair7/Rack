package fileidea.rack.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompRepository extends JpaRepository<Comp, Long> {

    List<Comp> findByPriceEstimateId(Long priceEstimateId);
}
