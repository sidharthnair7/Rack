package fileidea.rack.domain;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainService domainService;
    private final CustomDomainFilter domains;

    public DomainController(DomainService domainService, CustomDomainFilter domains) {
        this.domainService = domainService;
        this.domains = domains;
    }

    /**
     * Caddy's on-demand TLS gate: may we get a certificate for this hostname?
     *
     * <p>On-demand issuance is what lets a seller's domain work the moment its DNS points here,
     * with no redeploy and no hand-edited server config - which is the only version of "register a
     * domain and your shop is live on it" that is true for anyone but us.
     *
     * <p>It has to be gated. Without this check anyone could aim a hostname at this IP and make
     * the server request certificates on their behalf, which burns the ACME rate limit for
     * everyone on the box and eventually stops issuing for real sellers. Answering 200 only for a
     * domain some store actually registered keeps issuance bounded by the product.
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String domain) {
        String host = domain == null ? "" : domain.strip().toLowerCase(Locale.ROOT);
        return domains.resolve(host).isPresent()
                ? ResponseEntity.ok("ok")
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("unknown domain");
    }

    @PostMapping("/search")
    public List<String> search(@RequestParam String query) {
        return domainService.search(query);
    }

    @PostMapping("/register")
    public DomainService.RegistrationResult register(@RequestParam Long storeId, @RequestParam String domain) {
        return domainService.register(storeId, domain);
    }

    /** Point a domain the seller already owns at their shop. No registrar call, just the mapping. */
    @PostMapping("/connect")
    public DomainService.RegistrationResult connect(@RequestParam Long storeId, @RequestParam String domain) {
        return domainService.connect(storeId, domain);
    }
}
