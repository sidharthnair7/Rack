package fileidea.rack.listing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findByItemId(Long itemId);
}
