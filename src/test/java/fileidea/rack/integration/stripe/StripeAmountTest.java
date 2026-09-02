package fileidea.rack.integration.stripe;

import fileidea.rack.config.StripeProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Money conversion is the classic place to lose or multiply a factor of 100, and the mistake is
 * invisible until someone is charged the wrong amount.
 */
class StripeAmountTest {

    @Test
    void dollarsConvertToCents() {
        assertEquals(5500L, StripeHttpClient.toMinorUnits(new BigDecimal("55.00")));
        assertEquals(5500L, StripeHttpClient.toMinorUnits(new BigDecimal("55")));
        assertEquals(11000L, StripeHttpClient.toMinorUnits(new BigDecimal("110.0")));
        assertEquals(6299L, StripeHttpClient.toMinorUnits(new BigDecimal("62.99")));
    }

    @Test
    void fractionalCentsRoundRatherThanThrow() {
        assertEquals(6300L, StripeHttpClient.toMinorUnits(new BigDecimal("62.995")));
        assertEquals(6299L, StripeHttpClient.toMinorUnits(new BigDecimal("62.994")));
    }

    @Test
    void withoutAKeyThereIsNoCheckoutAndNoException() {
        StripeClient offline = new StripeHttpClient(null, null,
                new StripeProperties(null, "https://api.stripe.com", "usd", ""));
        assertNull(offline.createCheckoutLink("Levi's 501", "desc", new BigDecimal("55.00"), null),
                "an unconfigured Stripe must degrade to 'contact the seller', never fail the publish");
    }

    @Test
    void aZeroOrNegativePriceNeverBecomesACheckout() {
        StripeClient configured = new StripeHttpClient(null, null,
                new StripeProperties("sk_test_dummy", "https://api.stripe.com", "usd", ""));
        assertNull(configured.createCheckoutLink("x", "y", BigDecimal.ZERO, null));
        assertNull(configured.createCheckoutLink("x", "y", new BigDecimal("-5"), null));
        assertNull(configured.createCheckoutLink("x", "y", null, null));
    }

    /**
     * Stripe fetches the photo from outside, so a path that only resolves on the seller's own
     * machine has to resolve to nothing. Sending one produces a checkout page with a broken
     * thumbnail, which is worse than the grey placeholder it was meant to replace.
     */
    @Test
    void anImageStripeCouldNotReachResolvesToNothing() {
        StripeProperties local = new StripeProperties("sk_test_dummy", "https://api.stripe.com", "usd", "");
        assertNull(local.publicUrlFor("/uploads/items/4/on_model.jpg"), "no public address configured");

        StripeProperties loopback = new StripeProperties(
                "sk_test_dummy", "https://api.stripe.com", "usd", "http://localhost:8080");
        assertNull(loopback.publicUrlFor("/uploads/items/4/on_model.jpg"), "localhost is not reachable by Stripe");

        StripeProperties live = new StripeProperties(
                "sk_test_dummy", "https://api.stripe.com", "usd", "https://rackai.store");
        assertNull(live.publicUrlFor(null), "an item with no photo of that kind yet");
        assertNull(live.publicUrlFor("  "));
    }

    @Test
    void aPublicAddressTurnsAStoredPathIntoSomethingStripeCanFetch() {
        StripeProperties live = new StripeProperties(
                "sk_test_dummy", "https://api.stripe.com", "usd", "https://rackai.store");
        assertEquals("https://rackai.store/uploads/items/4/on_model.jpg",
                live.publicUrlFor("/uploads/items/4/on_model.jpg"));

        // A trailing slash on the base and a missing leading slash on the path are both things a
        // deployment env var realistically carries; neither should produce a doubled or missing one.
        StripeProperties trailing = new StripeProperties(
                "sk_test_dummy", "https://api.stripe.com", "usd", "https://rackai.store/");
        assertEquals("https://rackai.store/uploads/a.jpg", trailing.publicUrlFor("/uploads/a.jpg"));
        assertEquals("https://rackai.store/uploads/a.jpg", trailing.publicUrlFor("uploads/a.jpg"));

        // Already absolute (an S3 or CDN move later) passes straight through.
        assertEquals("https://cdn.example.com/a.jpg", live.publicUrlFor("https://cdn.example.com/a.jpg"));
    }
}
