package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rack.serpapi")
public record SerpApiProperties(
        String apiKey,
        @DefaultValue("cache/serpapi") String cacheDir,
        @DefaultValue("false") boolean cacheOnly
) {
    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
