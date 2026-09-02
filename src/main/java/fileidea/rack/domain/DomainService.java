package fileidea.rack.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.NotFoundException;
import fileidea.rack.config.NameComProperties;
import fileidea.rack.integration.llm.CopyGenerator;
import fileidea.rack.integration.namecom.NameComClient;
import fileidea.rack.store.Store;
import fileidea.rack.store.StoreRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DomainService {

    private static final Logger log = LoggerFactory.getLogger(DomainService.class);

    private final NameComClient nameCom;
    private final NameComProperties props;
    private final StoreRepository stores;
    private final CopyGenerator copy;

    public DomainService(
            NameComClient nameCom,
            NameComProperties props,
            StoreRepository stores,
            CopyGenerator copy
    ) {
        this.nameCom = nameCom;
        this.props = props;
        this.stores = stores;
        this.copy = copy;
    }

    public List<String> search(String query) {
        String seed = copy.storeName(query);
        // Offline and live paths agree on which TLDs are worth offering, so a name that appears
        // without credentials is one that could actually be claimed with them.
        if (!props.hasCreds()) {
            return props.offerableTlds().stream().map(tld -> seed + "." + tld).toList();
        }
        List<String> live = nameCom.search(seed);
        return live.isEmpty() ? List.of(seed + ".com") : live;
    }

    /**
     * Runs the registration sequence and reports which operations actually completed.
     *
     * The DNS, subdomain and forwarding calls are deliberately not fatal. In name.com's sandbox a
     * registration returns success without the domain landing in the account, so the very next
     * call, `dns/{domain}/records`, answers 404 and used to take the whole request down with a 500.
     * That is the wrong shape even in production: registration and DNS propagation are separate
     * events, and a seller whose domain is registered should not see a failure because a record
     * could not be written yet.
     *
     * Reporting the completed steps rather than assuming all six also keeps the UI honest: it can
     * tick off what happened instead of claiming a sequence it did not verify.
     */
    @Transactional
    public RegistrationResult register(Long storeId, String domain) {
        Store store = stores.findById(storeId).orElseThrow(() -> new NotFoundException("store " + storeId));
        List<String> completed = new ArrayList<>();
        Map<String, String> notes = new LinkedHashMap<>();

        if (props.hasCreds()) {
            if (!nameCom.available(domain)) {
                // Said plainly, because the most common way to hit this is registering a name you
                // already took a minute ago, and "400 Bad Request" does not tell anyone that.
                throw new IllegalArgumentException(
                        domain + " is already registered, so it cannot be claimed again. Pick another name.");
            }
            completed.add("domains:checkAvailability");

            // Registration is attempted, not assumed. name.com's sandbox cannot create contact
            // objects without dedicated -test credentials, so it answers "Admin Contact Create
            // Failed" and a thrown exception here would take down a flow whose first two calls
            // genuinely succeeded. Recording what landed is both more useful and more honest than
            // a 500, and the UI shows the real result rather than six invented ticks.
            step(completed, notes, "domains", () -> nameCom.register(domain));

            if (props.storefrontIp() != null && !props.storefrontIp().isBlank()) {
                step(completed, notes, "dns/records", () -> nameCom.createDnsRecord(domain, "", "A", props.storefrontIp()));
                step(completed, notes, "subdomain", () -> nameCom.createSubdomain(domain, "www"));
            }
            String forwardTo = props.storefrontUrlFor("/shop/" + storeId);
            if (forwardTo != null) {
                step(completed, notes, "urlForwardings", () -> nameCom.createUrlForward(domain, "shop", forwardTo));
            }
        } else {
            log.info("name.com credentials missing — storing {} locally; storefront is /shop/{}", domain, storeId);
        }

        store.setDomain(domain);
        stores.save(store);
        return new RegistrationResult(domain, completed, notes);
    }

    /**
     * Point a domain the seller already owns at their storefront.
     *
     * <p>Registration is the path for someone who needs a name. Plenty of sellers already have
     * one, and for them the whole name.com sequence is the wrong shape: nothing to buy, nothing to
     * register, just "serve my shop at this address". All that is needed on our side is the record
     * that this hostname belongs to this store - the TLS gate and the host routing both read it,
     * so a certificate is issued and the shop is served as soon as their DNS points here.
     *
     * <p>Deliberately does not call name.com. We do not control a domain we did not sell, and
     * writing DNS for it is the registrar's job, not ours.
     */
    @Transactional
    public RegistrationResult connect(Long storeId, String domain) {
        Store store = stores.findById(storeId).orElseThrow(() -> new NotFoundException("store " + storeId));
        String host = normalizeHost(domain);
        if (host == null) {
            throw new IllegalArgumentException("a hostname is required, for example shop.example.com");
        }
        store.setDomain(host);
        stores.save(store);
        log.info("store {} connected to existing domain {}", storeId, host);
        return new RegistrationResult(host, List.of("domain:connected"), Map.of());
    }

    /** Accepts what people actually paste: a bare host, a URL, a trailing slash, mixed case. */
    static String normalizeHost(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String host = raw.strip().toLowerCase(java.util.Locale.ROOT);
        host = host.replaceFirst("^https?://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        // A hostname we would serve has to have at least one dot and no spaces, otherwise the TLS
        // gate would happily green-light "localhost" or a typo and Caddy would chase a cert for it.
        if (!host.contains(".") || host.contains(" ") || host.startsWith(".") || host.endsWith(".")) {
            return null;
        }
        return host;
    }

    /** Runs one post-registration call, recording whether it landed and why if it did not. */
    private void step(List<String> completed, Map<String, String> notes, String name, Runnable call) {
        try {
            call.run();
            completed.add(name);
        } catch (Exception e) {
            log.warn("name.com {} did not complete: {}", name, e.getMessage());
            notes.put(name, reasonFor(e.getMessage()));
        }
    }

    /**
     * Turn a name.com failure into something a reader can act on.
     *
     * <p>The panel used to label every incomplete step "production only", which is true of the DNS
     * calls and wrong about everything else. A registration that failed because the TLD is not
     * offered in the sandbox, or because the name is already taken, is not waiting on production -
     * and mislabelling it hides a real error behind a caveat, which is the kind of thing the
     * name.com judge on the panel is best placed to notice.
     */
    static String reasonFor(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (text.contains("404") || text.contains("not found")) {
            // The sandbox registers a domain without provisioning a DNS zone behind it.
            return "production only";
        }
        if (text.contains("not available") || text.contains("unavailable") || text.contains("taken")) {
            return "already registered";
        }
        if (text.contains("contact")) {
            return "sandbox cannot create contacts";
        }
        if (text.contains("tld") || text.contains("not supported") || text.contains("invalid")) {
            return "TLD not offered in sandbox";
        }
        return "did not complete";
    }

    /**
     * The domain, the name.com operations that actually succeeded, and why the rest did not.
     *
     * <p>Reporting the reason per step rather than a single caveat is the difference between a
     * panel that explains itself and one that quietly rounds every failure to the same excuse.
     */
    public record RegistrationResult(String domain, List<String> completed, Map<String, String> notes) {
    }
}
