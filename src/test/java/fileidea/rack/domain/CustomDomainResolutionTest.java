package fileidea.rack.domain;

import fileidea.rack.store.Store;
import fileidea.rack.store.StoreRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Hostname to store is the lookup that turns a registered domain into an address rather than a
 * receipt. Getting it wrong in either direction is bad: miss a match and the seller's own domain
 * shows Rack's marketing page, match too loosely and one shop answers for another's hostname.
 */
class CustomDomainResolutionTest {

    private static CustomDomainFilter filterFor(String... registered) {
        List<String> domains = List.of(registered);
        StoreRepository repo = mock(StoreRepository.class);
        when(repo.findByDomainIgnoreCase(anyString())).thenAnswer(call -> {
            String asked = call.getArgument(0);
            return domains.stream()
                    .filter(d -> d.equalsIgnoreCase(asked))
                    .findFirst()
                    .map(d -> {
                        Store store = new Store();
                        store.setId((long) (domains.indexOf(d) + 1));
                        store.setDomain(d);
                        return store;
                    });
        });
        return new CustomDomainFilter(repo);
    }

    @Test
    void theRegisteredDomainResolvesToItsStore() {
        assertTrue(filterFor("sidshops.store").resolve("sidshops.store").isPresent());
    }

    @Test
    void hostnamesAreCaseInsensitiveAndPortsAreIgnored() {
        CustomDomainFilter filter = filterFor("sidshops.store");
        assertTrue(filter.resolve("SidShops.Store").isPresent(), "DNS does not preserve case");
        assertTrue(filter.resolve("sidshops.store:8080").isPresent(), "a port is not part of the name");
    }

    /**
     * Registration writes an A record for the apex and a www subdomain, so both forms arrive at
     * the server. They are the same shop, and only one row is stored.
     */
    @Test
    void theWwwFormFindsTheSameStore() {
        assertTrue(filterFor("sidshops.store").resolve("www.sidshops.store").isPresent());
    }

    @Test
    void anUnknownHostIsNotAStoreAndMustFallThrough() {
        CustomDomainFilter filter = filterFor("sidshops.store");
        assertTrue(filter.resolve("rackai.store").isEmpty(),
                "Rack's own marketing domain is not a storefront and must keep serving the landing page");
        assertTrue(filter.resolve("someone-elses-domain.com").isEmpty());
        assertTrue(filter.resolve(null).isEmpty());
        assertTrue(filter.resolve("  ").isEmpty());
        assertTrue(filter.resolve(":8080").isEmpty(), "a host that is only a port is not a name");
    }

    @Test
    void anExactMatchIsPreferredOverStrippingWww() {
        CustomDomainFilter filter = filterFor("www.shop.com", "shop.com");
        assertEquals("www.shop.com", filter.resolve("www.shop.com").orElseThrow().getDomain());
        assertEquals("shop.com", filter.resolve("shop.com").orElseThrow().getDomain());
    }

    @Test
    void theTlsGateAnswersOnlyForDomainsAStoreRegistered() {
        CustomDomainFilter filter = filterFor("sidshops.store");
        DomainController controller = new DomainController(null, filter);

        assertEquals(200, controller.verify("sidshops.store").getStatusCode().value());
        assertEquals(200, controller.verify("WWW.SidShops.Store").getStatusCode().value());
        // Anyone can point a hostname at this IP; only registered ones may trigger issuance.
        assertEquals(404, controller.verify("attacker-controlled.com").getStatusCode().value());
    }
}
