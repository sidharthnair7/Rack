package fileidea.rack.store;

import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.StoreResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    public StoreResponse create(@RequestParam Long sellerId, @RequestParam String name) {
        return Dtos.store(storeService.create(sellerId, name));
    }

    @GetMapping("/{id}")
    public StoreResponse get(@PathVariable Long id) {
        return Dtos.store(storeService.get(id));
    }
}
