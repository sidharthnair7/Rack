package fileidea.rack.domain;

import fileidea.rack.store.Store;
import fileidea.rack.store.StoreRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * Serves a seller's storefront at the domain they registered.
 *
 * <p>Registration alone was only ever half the promise. The API would buy the name, write the A
 * record and set up forwarding, and then a visitor typing that address got Rack's own marketing
 * page, because the storefront only existed at the path {@code /shop/{id}}. The domain was a
 * receipt rather than an address, which is the opposite of the pitch: your own storefront, on a
 * domain you own.
 *
 * <p>So the hostname is resolved to a store and the root path is served as that store's shop.
 * Only the root is rewritten - everything else (the API, uploaded images, the built frontend's
 * assets, and {@code /shop/...} itself) is left alone, so a custom domain gets a working shop
 * without shadowing any of the paths the app already serves.
 *
 * <p>Runs as a forward rather than a redirect deliberately. A redirect would bounce the visitor
 * off the seller's domain and onto {@code rackai.store/shop/1}, which would undo the thing the
 * seller registered a domain for in the first place.
 */
@Component
public class CustomDomainFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CustomDomainFilter.class);

    private final StoreRepository stores;

    public CustomDomainFilter(StoreRepository stores) {
        this.stores = stores;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!"/".equals(path) && !path.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Store> store = resolve(request.getServerName());
        if (store.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Long storeId = store.get().getId();
        log.debug("serving store {} for host {}", storeId, request.getServerName());
        request.getRequestDispatcher("/shop/" + storeId).forward(request, response);
    }

    /**
     * Match a hostname to a store, tolerating the two forms the same address arrives in.
     *
     * <p>{@code www.sidshops.store} and {@code sidshops.store} are the same shop to everyone
     * except a string comparison, and the registration flow writes an A record for both. Storing
     * only the bare name and stripping the prefix here keeps that a single row.
     */
    Optional<Store> resolve(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        String name = host.strip().toLowerCase(Locale.ROOT);
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(0, colon);
        }
        if (name.isEmpty()) {
            return Optional.empty();
        }
        Optional<Store> exact = stores.findByDomainIgnoreCase(name);
        if (exact.isPresent()) {
            return exact;
        }
        if (name.startsWith("www.")) {
            return stores.findByDomainIgnoreCase(name.substring(4));
        }
        return Optional.empty();
    }
}
