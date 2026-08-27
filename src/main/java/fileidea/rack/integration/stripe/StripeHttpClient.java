package fileidea.rack.integration.stripe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import fileidea.rack.config.StripeProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Two calls: create a Price (with the product declared inline), then a Payment Link for it.
 *
 * <p>Stripe's API is form-encoded, not JSON, and nests parameters with bracket syntax
 * ({@code line_items[0][price]}). Shipping address collection is enabled so the seller gets a
 * delivery address with the payment — the same thing a marketplace would hand them.
 */
@Component
public class StripeHttpClient implements StripeClient {

    private static final Logger log = LoggerFactory.getLogger(StripeHttpClient.class);

    private final RestClient http;
    private final JsonMapper mapper;
    private final StripeProperties props;

    public StripeHttpClient(RestClient http, JsonMapper mapper, StripeProperties props) {
        this.http = http;
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public String createCheckoutLink(String productName, String description, BigDecimal amount) {
        if (!props.hasKey()) {
            return null;
        }
        if (amount == null || amount.signum() <= 0) {
            return null;
        }
        try {
            String priceId = createPrice(productName, description, amount);
            return createPaymentLink(priceId);
        } catch (Exception e) {
            // A checkout is an enhancement to the listing, never a gate on it. If Stripe is
            // unreachable the item still publishes and the storefront falls back to contact details.
            log.warn("could not create a checkout link for \"{}\": {}", productName, e.getMessage());
            return null;
        }
    }

    private String createPrice(String productName, String description, BigDecimal amount) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("currency", props.currency());
        form.add("unit_amount", String.valueOf(toMinorUnits(amount)));
        form.add("product_data[name]", trim(productName, 250, "Secondhand item"));
        if (description != null && !description.isBlank()) {
            form.add("product_data[metadata][description]", trim(description, 490, ""));
        }
        JsonNode price = post("/v1/prices", form);
        String id = text(price, "id");
        if (id == null) {
            throw new IllegalStateException("Stripe price response had no id: " + price);
        }
        return id;
    }

    private String createPaymentLink(String priceId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("line_items[0][price]", priceId);
        form.add("line_items[0][quantity]", "1");
        form.add("submit_type", "pay");
        // The seller has to post the parcel somewhere, so collect the address at checkout.
        form.add("shipping_address_collection[allowed_countries][0]", "US");
        form.add("shipping_address_collection[allowed_countries][1]", "CA");
        form.add("shipping_address_collection[allowed_countries][2]", "GB");
        // Every item is one-of-a-kind: once it sells, the link stops accepting payments.
        form.add("restrictions[completed_sessions][limit]", "1");

        JsonNode link = post("/v1/payment_links", form);
        String url = text(link, "url");
        if (url == null) {
            throw new IllegalStateException("Stripe payment link response had no url: " + link);
        }
        return url;
    }

    /** Stripe takes amounts in the currency's smallest unit, so $55.00 is 5500. */
    static long toMinorUnits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private JsonNode post(String path, MultiValueMap<String, String> form) {
        String json = http.post()
                .uri(props.baseUrl() + path)
                .header("Authorization", "Bearer " + props.secretKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        return mapper.readTree(json == null ? "{}" : json);
    }

    private static String trim(String value, int max, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String clean = value.strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asString().isBlank()) {
            return null;
        }
        return value.asString();
    }
}
