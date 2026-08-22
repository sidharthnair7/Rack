package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rack.perfectcorp")
public record PerfectCorpProperties(
        String apiKey,
        @DefaultValue("https://yce-api-01.makeupar.com") String baseUrl,
        @DefaultValue("https://plugins-media.makeupar.com/strapi/assets/clothes_03_cccd5d4803.jpeg") String modelUrl
) {
    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
