package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rack.stripe")
public record StripeProperties(
        String secretKey,
        @DefaultValue("https://api.stripe.com") String baseUrl,
        @DefaultValue("usd") String currency
) {
    public boolean hasKey() {
        return secretKey != null && !secretKey.isBlank();
    }
}
