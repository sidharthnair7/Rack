package fileidea.rack.domain;

import fileidea.rack.config.NameComProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * name.com's sandbox refuses to register {@code .shop} - it cannot create the contact objects that
 * TLD requires - while its own search suggests {@code .shop} for almost every query. Ten of eleven
 * results for one real query were names that could not be claimed, so nearly every button in the
 * panel was one that failed when clicked.
 *
 * <p>Offering an option that always fails is a broken control, not an honest disclosure. In
 * production every TLD works and nothing is filtered.
 */
class OfferableTldTest {

    private static NameComProperties at(String baseUrl) {
        return new NameComProperties("user", "token", baseUrl, "1.2.3.4", "https://rackai.store");
    }

    @Test
    void theSandboxOnlyOffersWhatItCanActuallyRegister() {
        NameComProperties sandbox = at("https://api.dev.name.com");
        assertTrue(sandbox.sandbox());
        assertEquals(java.util.List.of("com", "store"), sandbox.offerableTlds());
        assertFalse(sandbox.offerableTlds().contains("shop"),
                "the sandbox cannot create contacts for .shop, so it must never be suggested");
    }

    @Test
    void productionFiltersNothing() {
        NameComProperties live = at("https://api.name.com");
        assertFalse(live.sandbox());
        assertTrue(live.offerableTlds().contains("shop"),
                ".shop registers normally against the real registrar");
    }
}
