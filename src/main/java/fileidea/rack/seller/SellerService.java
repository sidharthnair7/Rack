package fileidea.rack.seller;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.NotFoundException;

@Service
public class SellerService {

    private final SellerRepository sellers;

    public SellerService(SellerRepository sellers) {
        this.sellers = sellers;
    }

    @Transactional
    public Seller create(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        Seller seller = new Seller();
        seller.setEmail(email.trim().toLowerCase());
        seller.setPasswordHash("{noop}demo");
        return sellers.save(seller);
    }

    @Transactional(readOnly = true)
    public Seller get(Long id) {
        return sellers.findById(id).orElseThrow(() -> new NotFoundException("seller " + id));
    }
}
