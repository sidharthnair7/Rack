package fileidea.rack.identify;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Google Lens only returns a knowledge_graph for catalogued products. Secondhand clothing on a
 * bed — the actual input this product takes — usually comes back with visual_matches and nothing
 * else, so the fallback path is the common case, not the edge case.
 */
class LensFallbackTest {

    private final IdentifyService identify = new IdentifyService(null, null, null, null);
    private final JsonMapper mapper = JsonMapper.shared();

    private JsonNode fixture(String name) throws Exception {
        try (var in = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return mapper.readTree(in);
        }
    }

    @Test
    void identifiesFromVisualMatchesWhenThereIsNoKnowledgeGraph() throws Exception {
        Identification guess = identify.interpretLens(fixture("lens-no-knowledge-graph.json"));

        assertNotNull(guess.brand(), "no brand means an empty eBay query and therefore no comps");
        assertFalse(guess.brand().isBlank());
        assertTrue(guess.brand().toLowerCase().contains("carhartt"), "got: " + guess.brand());
        assertEquals("Jacket", guess.garmentType());
        assertEquals("Outerwear", guess.category());
    }

    @Test
    void knowledgeGraphStillWinsWhenPresent() throws Exception {
        Identification guess = identify.interpretLens(fixture("lens-levis.json"));
        assertTrue(guess.brand().toLowerCase().contains("levi"), "got: " + guess.brand());
        assertEquals("Jeans", guess.garmentType());
        assertEquals("Bottoms", guess.category());
    }

    @Test
    void emptyResponseDoesNotInventAnything() {
        Identification guess = identify.interpretLens(mapper.readTree("{}"));
        assertNull(guess.brand());
        assertNull(guess.garmentType());
        assertNull(guess.category());
    }

    @Test
    void picksTheBrandThatRecursRatherThanTheFirstWord() {
        String brand = IdentifyService.recurringBrand(List.of(
                "Blue cotton shirt by Uniqlo",
                "Uniqlo oxford button down",
                "Uniqlo U striped shirt"
        ));
        assertNotNull(brand);
        assertEquals("uniqlo", brand.toLowerCase());
    }

    @Test
    void singleNoisyTitleStillYieldsSomething() {
        String brand = IdentifyService.recurringBrand(List.of("Patagonia Better Sweater"));
        assertNotNull(brand, "one match is still better than an empty query");
    }
}
