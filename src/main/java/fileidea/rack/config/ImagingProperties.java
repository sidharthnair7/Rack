package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rack.imaging")
public record ImagingProperties(
        boolean backgroundRemoval,
        boolean lighting,
        boolean enhance,
        boolean studio,
        boolean tryOn,

        /**
         * Split a photo holding several garments into one item each.
         *
         * <p>Costs one extra background-removal call per upload, before anything else runs, and
         * only pays off on a photo that actually holds more than one piece. A flag rather than a
         * constant because it is the only stage that changes how many items an upload produces,
         * so it has to be switchable without a rebuild if it ever misreads a photo.
         */
        boolean splitGarments
) {
}
