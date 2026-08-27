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
                new StripeProperties(null, "https://api.stripe.com", "usd"));
        assertNull(offline.createCheckoutLink("Levi's 501", "desc", new BigDecimal("55.00")),
                "an unconfigured Stripe must degrade to 'contact the seller', never fail the publish");
    }

    @Test
    void aZeroOrNegativePriceNeverBecomesACheckout() {
        StripeClient configured = new StripeHttpClient(null, null,
                new StripeProperties("sk_test_dummy", "https://api.stripe.com", "usd"));
        assertNull(configured.createCheckoutLink("x", "y", BigDecimal.ZERO));
        assertNull(configured.createCheckoutLink("x", "y", new BigDecimal("-5")));
        assertNull(configured.createCheckoutLink("x", "y", null));
    }
}
