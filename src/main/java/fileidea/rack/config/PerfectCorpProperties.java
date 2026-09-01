package fileidea.rack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rack.perfectcorp")
public record PerfectCorpProperties(
        String apiKey,
        @DefaultValue("https://yce-api-01.makeupar.com") String baseUrl,
        String modelUrl
) {

    /**
     * Perfect Corp's own sample garment model. Kept only so the try-on stage still runs before a
     * synthetic model has been generated. It is not a suitable thing to ship: it is a stock
     * photograph of a real person hosted by the vendor, so publishing with it both breaks the
     * project's own rule that no photograph of a real person exists anywhere in the system, and
     * shows Perfect Corp their own sample asset in a submission to their challenge.
     */
    private static final String VENDOR_SAMPLE_MODEL =
            "https://plugins-media.makeupar.com/strapi/assets/clothes_03_cccd5d4803.jpeg";

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * True once a real synthetic model has been configured.
     * <p>
     * Checked explicitly rather than relying on {@code @DefaultValue}, because that only applies
     * when a property is <em>absent</em>. {@code RACK_PERFECTCORP_MODEL_URL=} in a .env file is a
     * present, empty value, so it binds as "" and wins over the annotation. That is the same
     * class of bug that once made a real key in .env get ignored.
     */
    public boolean hasOwnModel() {
        return modelUrl != null && !modelUrl.isBlank();
    }

    /** The URL to send as the try-on subject, falling back to the vendor sample. */
    public String resolvedModelUrl() {
        return hasOwnModel() ? modelUrl.trim() : VENDOR_SAMPLE_MODEL;
    }
}
