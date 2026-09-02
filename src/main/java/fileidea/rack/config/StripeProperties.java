package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rack.stripe")
public record StripeProperties(
        String secretKey,
        @DefaultValue("https://api.stripe.com") String baseUrl,
        @DefaultValue("usd") String currency,
        @DefaultValue("") String publicBaseUrl
) {
    public boolean hasKey() {
        return secretKey != null && !secretKey.isBlank();
    }

    /**
     * Turn a stored image path into something Stripe can actually fetch.
     *
     * <p>Image assets are stored as site-relative paths ({@code /uploads/items/4/on_model.jpg})
     * because that is what the browser needs. Stripe is a third party retrieving the file from
     * outside, so it needs an absolute public URL and silently drops one it cannot resolve.
     *
     * <p>Returns {@code null} when the app has no public address. On localhost there is nothing
     * Stripe could reach, and sending {@code http://localhost:8080/...} would produce a checkout
     * page with a broken thumbnail rather than no thumbnail.
     */
    public String publicUrlFor(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String clean = path.strip();
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean;
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        String base = publicBaseUrl.strip();
        if (base.startsWith("http://localhost") || base.startsWith("https://localhost")) {
            return null;
        }
        return base.replaceAll("/+$", "") + (clean.startsWith("/") ? clean : "/" + clean);
    }
}
