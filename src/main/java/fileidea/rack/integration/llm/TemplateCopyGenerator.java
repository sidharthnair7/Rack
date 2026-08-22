package fileidea.rack.integration.llm;

import org.springframework.stereotype.Component;

@Component
public class TemplateCopyGenerator implements CopyGenerator {

    @Override
    public String storeName(String seed) {
        if (seed == null || seed.isBlank()) {
            return "closet";
        }
        return seed.toLowerCase().replaceAll("[^a-z0-9]+", "").strip();
    }

    @Override
    public String listingTitle(String brand, String type, String category) {
        String b = blankTo(brand, "Vintage");
        String t = blankTo(type, blankTo(category, "piece"));
        return b + " " + t;
    }

    @Override
    public String listingDescription(String brand, String type, String category, String condition) {
        return listingTitle(brand, type, category)
                + " in "
                + blankTo(condition, "good")
                + " condition. Photographed from a closet pile, cleaned up for the listing. "
                + "Price is from recent sold comps, not a guess.";
    }

    @Override
    public String normalizeBrand(String rawLensOutput) {
        if (rawLensOutput == null) {
            return "";
        }
        return rawLensOutput.replaceAll("\\s+", " ").strip();
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
