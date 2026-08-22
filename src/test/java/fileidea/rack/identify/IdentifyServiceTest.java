package fileidea.rack.identify;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifyServiceTest {

    private final IdentifyService identify = new IdentifyService(null, null, null, null);
    private final JsonMapper mapper = JsonMapper.shared();

    @Test
    void readsBrandAndTypeOutOfNoisyLensJson() throws Exception {
        try (var in = getClass().getResourceAsStream("/fixtures/lens-levis.json")) {
            JsonNode lens = mapper.readTree(in);
            Identification guess = identify.interpretLens(lens);
            assertNotNull(guess);
            assertNotNull(guess.brand());
            assertFalse(guess.brand().isBlank());
            assertTrue(guess.brand().toLowerCase().contains("levi"),
                    "expected a Levi's guess, got: " + guess.brand());
            assertNotNull(guess.garmentType());
            assertFalse(guess.garmentType().isBlank());
        }
    }
}
