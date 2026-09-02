package fileidea.rack.domain;

import fileidea.rack.config.NameComProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A URL forward is an HTTP redirect, so the destination has to be somewhere a browser can go.
 *
 * <p>The bare path {@code /shop/1} was being sent as that destination. name.com's sandbox answers
 * 404 on the forwarding endpoint before it ever validates the body, so nothing here complained -
 * the mistake would have surfaced in production, on a domain that had already been paid for.
 */
class ForwardDestinationTest {

    private static NameComProperties props(String ip, String url) {
        return new NameComProperties("user", "token", "https://api.dev.name.com", ip, url);
    }

    @Test
    void aConfiguredHostnameWins() {
        assertEquals("https://rackai.store/shop/1",
                props("15.157.179.11", "https://rackai.store").storefrontUrlFor("/shop/1"));
    }

    @Test
    void withoutAHostnameItStillForwardsSomewhereReal() {
        assertEquals("http://15.157.179.11/shop/1",
                props("15.157.179.11", "").storefrontUrlFor("/shop/1"));
    }

    @Test
    void theDestinationIsNeverARelativePath() {
        String destination = props("15.157.179.11", "https://rackai.store").storefrontUrlFor("/shop/1");
        assertTrue(destination.startsWith("http://") || destination.startsWith("https://"),
                "name.com cannot redirect a browser to a path with no host: " + destination);
    }

    @Test
    void trailingAndMissingSlashesDoNotDoubleUp() {
        assertEquals("https://rackai.store/shop/2",
                props(null, "https://rackai.store/").storefrontUrlFor("/shop/2"));
        assertEquals("https://rackai.store/shop/2",
                props(null, "https://rackai.store").storefrontUrlFor("shop/2"));
    }

    @Test
    void nothingConfiguredMeansNoForwardRatherThanABrokenOne() {
        assertNull(props(null, "").storefrontUrlFor("/shop/1"));
        assertNull(props("  ", "  ").storefrontUrlFor("/shop/1"));
    }
}
