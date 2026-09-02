package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rack.namecom")
public record NameComProperties(
        String username,
        String token,
        @DefaultValue("https://api.dev.name.com") String baseUrl,
        String storefrontIp,
        @DefaultValue("") String storefrontUrl
) {
    public boolean hasCreds() {
        return username != null && !username.isBlank() && token != null && !token.isBlank();
    }

    /** True when pointed at name.com's test environment rather than the real registrar. */
    public boolean sandbox() {
        return baseUrl != null && baseUrl.contains("api.dev.name.com");
    }

    /**
     * TLDs worth offering the seller.
     *
     * <p>The sandbox registers {@code .com} and {@code .store} and refuses {@code .shop}, which it
     * rejects with "Admin Contact Create Failed" because it cannot create the contact objects that
     * TLD requires. name.com's own search happily suggests {@code .shop} anyway - ten of eleven
     * results for one query - so almost every name a seller could click was one that could not be
     * claimed. Offering an option that always fails is not honesty about a limitation, it is a
     * broken control.
     *
     * <p>In production every TLD works, so nothing is filtered there.
     */
    public java.util.List<String> offerableTlds() {
        return sandbox() ? java.util.List.of("com", "store") : java.util.List.of("com", "store", "shop");
    }

    /**
     * Absolute destination for a URL forward.
     *
     * <p>A forward is an HTTP redirect, so name.com needs somewhere to send the browser. This used
     * to pass the bare path {@code /shop/1}, which is not a destination at all. The sandbox
     * answers 404 on that endpoint before it ever validates the body, so the mistake was invisible
     * here and would have surfaced only in production, after paying for a real domain.
     *
     * <p>Prefers the configured public address, and falls back to the storefront IP so a
     * deployment that never set a hostname still forwards somewhere real rather than nowhere.
     */
    public String storefrontUrlFor(String path) {
        String suffix = path == null || path.isBlank() ? "" : (path.startsWith("/") ? path : "/" + path);
        if (storefrontUrl != null && !storefrontUrl.isBlank()) {
            return storefrontUrl.strip().replaceAll("/+$", "") + suffix;
        }
        if (storefrontIp != null && !storefrontIp.isBlank()) {
            return "http://" + storefrontIp.strip() + suffix;
        }
        return null;
    }
}
