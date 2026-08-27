package fileidea.rack.integration.serpapi;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface SerpApiClient {

    JsonNode lens(byte[] image);

    List<ShoppingResult> shopping(String query);

    /**
     * Comparable eBay listings for a query. These are items currently for sale, not completed
     * sales: eBay put sold/completed listings behind a login in July 2026, after which SerpApi's
     * show_only=Sold filter returns zero rows for every query (verified against live queries).
     * The name says "comps", not "sold", so nothing downstream can quietly assume otherwise.
     */
    List<EbayComp> ebayComps(String query);

    List<Integer> trendSeries(String brandTerm);

    record ShoppingResult(BigDecimal retailAnchor, String title, String sourceUrl) {
    }

    /**
     * @param soldDate populated only if eBay ever exposes completed-sale dates again; null for
     *                 the live listings this currently returns, and the UI omits it when null.
     */
    record EbayComp(String title, BigDecimal price, Instant soldDate, String sourceUrl) {
    }
}
