package fileidea.rack.integration.serpapi;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface SerpApiClient {

    JsonNode lens(byte[] image);

    List<ShoppingResult> shopping(String query);

    List<EbaySoldComp> ebaySold(String query);

    List<Integer> trendSeries(String brandTerm);

    record ShoppingResult(BigDecimal retailAnchor, String title, String sourceUrl) {
    }

    record EbaySoldComp(String title, BigDecimal price, Instant soldDate, String sourceUrl) {
    }
}
