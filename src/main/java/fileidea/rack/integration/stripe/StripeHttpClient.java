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
 * Three calls: create a Product, a Price against it, then a Payment Link.
 *
 * <p>Stripe's API is form-encoded, not JSON, and nests parameters with bracket syntax
 * ({@code line_items[0][price]}). Shipping address collection is enabled so the seller gets a
 * delivery address with the payment — the same thing a marketplace would hand them.
 *
 * <p>This used to declare the product inline on the Price, which is one fewer call but cannot
 * carry a photo: {@code /v1/prices} rejects {@code product_data[images]} outright, and the
 * description it does accept only lives in metadata, which Stripe never renders. The result was a
 * checkout page showing a grey placeholder and no description for a garment whose photograph is
 * the entire point of the product. Creating the Product first is the only way to attach either.
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
    public String createCheckoutLink(String productName, String description, BigDecimal amount, String imageUrl) {
        if (!props.hasKey()) {
            return null;
        }
        if (amount == null || amount.signum() <= 0) {
            return null;
        }
        try {
            String productId = createProduct(productName, description, imageUrl);
            String priceId = createPrice(productId, amount);
            return createPaymentLink(priceId);
        } catch (Exception e) {
            // A checkout is an enhancement to the listing, never a gate on it. If Stripe is
            // unreachable the item still publishes and the storefront falls back to contact details.
            log.warn("could not create a checkout link for \"{}\": {}", productName, e.getMessage());
            return null;
        }
    }

    private String createProduct(String productName, String description, String imageUrl) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", trim(productName, 250, "Secondhand item"));
        if (description != null && !description.isBlank()) {
            form.add("description", trim(description, 490, ""));
        }
        // Only ever an absolute public URL - the caller nulls this out rather than hand Stripe an
        // address it cannot reach. Stripe fetches the file itself when the Product is created.
        if (imageUrl != null && !imageUrl.isBlank()) {
            form.add("images[0]", imageUrl.strip());
        }
        JsonNode product = post("/v1/products", form);
        String id = text(product, "id");
        if (id == null) {
            throw new IllegalStateException("Stripe product response had no id: " + product);
        }
        return id;
    }

    private String createPrice(String productId, BigDecimal amount) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("currency", props.currency());
        form.add("unit_amount", String.valueOf(toMinorUnits(amount)));
        form.add("product", productId);
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
