package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rack.namecom")
public record NameComProperties(
        String username,
        String token,
        @DefaultValue("https://api.dev.name.com") String baseUrl,
        String storefrontIp
) {
    public boolean hasCreds() {
        return username != null && !username.isBlank() && token != null && !token.isBlank();
    }
}
