package fileidea.rack.integration.serpapi;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the exact JSON shapes SerpApi returns. The eBay engine nests price under
 * {@code price.extracted}; reading a flat {@code extracted_price} there silently dropped every
 * comp, which left the median null and stopped anything from ever being listed.
 */
class SerpApiResponseShapeTest {

    private final JsonMapper mapper = JsonMapper.shared();

    private JsonNode fixture(String name) throws Exception {
        try (var in = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return mapper.readTree(in);
        }
    }

    @Test
    void readsTheNestedEbayPriceObject() throws Exception {
        JsonNode hit = fixture("ebay-sold-levis.json").path("organic_results").get(0);
        BigDecimal price = SerpApiHttpClient.ebayPrice(hit);
        assertNotNull(price, "eBay prices live at price.extracted, not extracted_price");
        assertEquals(0, new BigDecimal("62.0").compareTo(price));
    }

    @Test
    void fallsBackToTheLowEndOfARangedPrice() throws Exception {
        JsonNode hit = fixture("ebay-sold-levis.json").path("organic_results").get(2);
        BigDecimal price = SerpApiHttpClient.ebayPrice(hit);
        assertNotNull(price);
        assertEquals(0, new BigDecimal("40.0").compareTo(price));
    }

    @Test
    void returnsNullRatherThanGuessingWhenThereIsNoPrice() throws Exception {
        JsonNode hit = fixture("ebay-sold-levis.json").path("organic_results").get(3);
        assertNull(SerpApiHttpClient.ebayPrice(hit));
    }

    @Test
    void parsesTheHumanReadableSoldDate() throws Exception {
        JsonNode hit = fixture("ebay-sold-levis.json").path("organic_results").get(0);
        Instant sold = SerpApiHttpClient.parseDate(hit);
        assertNotNull(sold, "eBay sold dates are strings like 'Sold  Aug 14, 2026', never ISO-8601");
        assertTrue(sold.toString().startsWith("2026-08-14"), "got " + sold);
    }

    @Test
    void stillParsesAnIsoDate() throws Exception {
        JsonNode hit = fixture("ebay-sold-levis.json").path("organic_results").get(2);
        Instant sold = SerpApiHttpClient.parseDate(hit);
        assertNotNull(sold);
        assertTrue(sold.toString().startsWith("2026-08-09"), "got " + sold);
    }
}
