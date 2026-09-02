package fileidea.rack.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Whatever a seller pastes has to end up as a bare hostname, because the same string is the TLS
 * gate's answer and the host-routing key. A value that slips through as "localhost" or a typo
 * would have Caddy chasing a certificate that can never be issued.
 */
class HostNormalisationTest {

    @Test
    void aPastedUrlBecomesAHostname() {
        assertEquals("shop.example.com", DomainService.normalizeHost("https://shop.example.com"));
        assertEquals("shop.example.com", DomainService.normalizeHost("http://shop.example.com/"));
        assertEquals("shop.example.com", DomainService.normalizeHost("https://shop.example.com/shop/1"));
        assertEquals("shop.example.com", DomainService.normalizeHost("  Shop.Example.COM  "));
        assertEquals("shop.example.com", DomainService.normalizeHost("shop.example.com:8443"));
    }

    @Test
    void somethingThatIsNotAServableHostnameIsRejected() {
        assertNull(DomainService.normalizeHost("localhost"), "no dot, and never publicly issuable");
        assertNull(DomainService.normalizeHost("my shop.com"));
        assertNull(DomainService.normalizeHost(".example.com"));
        assertNull(DomainService.normalizeHost("example.com."));
        assertNull(DomainService.normalizeHost(""));
        assertNull(DomainService.normalizeHost(null));
    }
}
