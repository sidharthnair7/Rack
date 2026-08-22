package fileidea.rack.domain;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @PostMapping("/search")
    public List<String> search(@RequestParam String query) {
        return domainService.search(query);
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestParam Long storeId, @RequestParam String domain) {
        return Map.of("domain", domainService.register(storeId, domain));
    }
}
