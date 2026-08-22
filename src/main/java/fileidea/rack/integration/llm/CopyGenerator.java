package fileidea.rack.integration.llm;

public interface CopyGenerator {

    String storeName(String seed);

    String listingTitle(String brand, String type, String category);

    String listingDescription(String brand, String type, String category, String condition);

    String normalizeBrand(String rawLensOutput);
}
