package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rack.tasks")
public record TaskProperties(
        boolean pollerEnabled,
        long pollIntervalMs
) {
}
