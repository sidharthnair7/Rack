package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rack.imaging")
public record ImagingProperties(
        boolean backgroundRemoval,
        boolean lighting,
        boolean enhance,
        boolean studio,
        boolean tryOn
) {
}
