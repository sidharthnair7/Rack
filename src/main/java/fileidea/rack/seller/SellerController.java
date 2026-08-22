package fileidea.rack.seller;

import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.SellerResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping
    public SellerResponse create(@RequestParam String email) {
        return Dtos.seller(sellerService.create(email));
    }

    @GetMapping("/{id}")
    public SellerResponse get(@PathVariable Long id) {
        return Dtos.seller(sellerService.get(id));
    }
}
