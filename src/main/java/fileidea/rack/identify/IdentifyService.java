package fileidea.rack.identify;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.common.Vendor;
import fileidea.rack.intake.Item;
import fileidea.rack.intake.ItemRepository;
import fileidea.rack.integration.serpapi.SerpApiClient;
import fileidea.rack.integration.storage.ImageStorage;
import fileidea.rack.task.TaskEndpoints;
import fileidea.rack.task.TaskOrchestrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class IdentifyService {

    private final ItemRepository items;
    private final ImageStorage storage;
    private final SerpApiClient serpApi;
    private final TaskOrchestrator tasks;

    public IdentifyService(
            ItemRepository items,
            ImageStorage storage,
            SerpApiClient serpApi,
            TaskOrchestrator tasks
    ) {
        this.items = items;
        this.storage = storage;
        this.serpApi = serpApi;
        this.tasks = tasks;
    }

    @Transactional
    public Item identify(Long itemId) {
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("item " + itemId));
        byte[] image = storage.read(storage.keyFromPublicUrl(item.getSourceImageUrl()));
        JsonNode lens = serpApi.lens(image);
        Identification guess = interpretLens(lens);
        item.setIdentifiedBrand(guess.brand());
        item.setIdentifiedType(guess.garmentType());
        item.setCategory(guess.category());
        item.setStatus(ItemStatus.IDENTIFIED);
        items.save(item);
        tasks.enqueue(item, Vendor.SERPAPI, TaskEndpoints.PRICE, TaskEndpoints.priceKey(item.getId()));
        return item;
    }

    @Transactional
    public Item correctBrand(Long itemId, String brand) {
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("brand is required");
        }
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("item " + itemId));
        item.setUserCorrectedBrand(brand.trim());
        Item saved = items.save(item);


        tasks.requeue(TaskEndpoints.priceKey(itemId));
        tasks.requeue(TaskEndpoints.publishKey(itemId));
        return saved;
    }


    /**
     * Google Lens only returns a knowledge_graph for items it recognises as a catalogued product.
     * Secondhand clothing on a bed usually is not, so the common real-world response has
     * visual_matches and nothing else. Reading knowledge_graph alone left brand and type blank,
     * which produced an empty eBay query and therefore no comps at all.
     *
     * Order: knowledge_graph when present, then the brand token that recurs across the
     * visual_matches titles, then a keyword scan for the garment type.
     */
    public Identification interpretLens(JsonNode lensJson) {
        if (lensJson == null) {
            return new Identification(null, null, null);
        }
        List<String> titles = matchTitles(lensJson);
        String brand = firstPresent(
                textAt(lensJson, "knowledge_graph", "title"),
                recurringBrand(titles)
        );
        String type = firstPresent(
                textAt(lensJson, "knowledge_graph", "type"),
                garmentTypeIn(titles)
        );
        return new Identification(brand, type, categoryFor(type));
    }

    private static List<String> matchTitles(JsonNode lensJson) {
        List<String> titles = new ArrayList<>();
        for (String field : List.of("visual_matches", "exact_matches", "products")) {
            JsonNode array = lensJson.path(field);
            if (!array.isArray()) {
                continue;
            }
            for (JsonNode hit : array) {
                String title = textAt(hit, "title");
                if (title != null) {
                    titles.add(title);
                }
                if (titles.size() >= 12) {
                    return titles;
                }
            }
        }
        return titles;
    }

    /**
     * The brand is the word that keeps showing up across independent listings of the same
     * garment. Counting tokens across match titles is more robust than trusting any single
     * result, and it needs no model call, so the answer is reproducible in a demo.
     */
    static String recurringBrand(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> display = new LinkedHashMap<>();
        for (String title : titles) {
            Set<String> seenInThisTitle = new HashSet<>();
            for (String word : title.split("[^\\p{Alnum}']+")) {
                String key = word.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                if (key.length() < 3 || STOPWORDS.contains(key) || TYPE_WORDS.containsKey(key)) {
                    continue;
                }
                if (key.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                if (seenInThisTitle.add(key)) {
                    counts.merge(key, 1, Integer::sum);
                    display.putIfAbsent(key, word);
                }
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(top -> top.getValue() >= 2)
                .map(top -> display.get(top.getKey()))
                .orElseGet(() -> display.values().stream().findFirst().orElse(null));
    }

    static String garmentTypeIn(List<String> titles) {
        if (titles == null) {
            return null;
        }
        for (String title : titles) {
            for (String word : title.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
                String canonical = TYPE_WORDS.get(word);
                if (canonical != null) {
                    return canonical;
                }
            }
        }
        return null;
    }

    /**
     * Category exists to route the item to the right Perfect Corp try-on endpoint, so it is a
     * fixed vocabulary rather than free text.
     */
    static String categoryFor(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String key = type.toLowerCase(Locale.ROOT).trim();
        return TYPE_CATEGORY.getOrDefault(TYPE_WORDS.getOrDefault(key, key).toLowerCase(Locale.ROOT), "Apparel");
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String textAt(JsonNode root, String... path) {
        JsonNode node = root;
        for (String segment : path) {
            if (node == null) {
                return null;
            }
            node = node.path(segment);
        }
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asString();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "size", "new", "used", "vintage", "original", "fit",
            "men", "mens", "women", "womens", "unisex", "kids", "boys", "girls", "authentic",
            "genuine", "style", "classic", "regular", "slim", "straight", "relaxed", "premium",
            "quality", "brand", "sale", "shop", "buy", "free", "shipping", "condition", "rare",
            "made", "usa", "small", "medium", "large", "xlarge", "colour", "color", "black",
            "white", "blue", "green", "grey", "gray", "brown", "beige", "navy", "denim",
            // Stock-photo boilerplate. A watermarked catalogue image makes "stock", "photo" and
            // the agency name the most-repeated tokens across visual matches, so the brand comes
            // back as "Stock" - which then searches eBay for stock photos rather than clothing.
            "stock", "photo", "photos", "picture", "image", "images", "royalty", "depositphotos",
            "shutterstock", "istock", "alamy", "getty", "adobe", "dreamstime", "vector",
            "isolated", "background", "closeup", "close", "top", "view", "flat", "lay", "mockup"
    );

    private static final Map<String, String> TYPE_WORDS = Map.ofEntries(
            Map.entry("jeans", "Jeans"), Map.entry("jean", "Jeans"),
            Map.entry("trousers", "Trousers"), Map.entry("pants", "Trousers"),
            Map.entry("chinos", "Trousers"), Map.entry("shorts", "Shorts"),
            Map.entry("skirt", "Skirt"), Map.entry("dress", "Dress"),
            Map.entry("shirt", "Shirt"), Map.entry("blouse", "Shirt"),
            Map.entry("tee", "T-Shirt"), Map.entry("tshirt", "T-Shirt"),
            Map.entry("top", "Top"), Map.entry("sweater", "Sweater"),
            Map.entry("jumper", "Sweater"), Map.entry("cardigan", "Sweater"),
            Map.entry("hoodie", "Hoodie"), Map.entry("sweatshirt", "Hoodie"),
            Map.entry("jacket", "Jacket"), Map.entry("coat", "Coat"),
            Map.entry("blazer", "Jacket"), Map.entry("parka", "Coat"),
            Map.entry("sneakers", "Sneakers"), Map.entry("sneaker", "Sneakers"),
            Map.entry("trainers", "Sneakers"), Map.entry("shoes", "Shoes"),
            Map.entry("boots", "Boots"), Map.entry("boot", "Boots"),
            Map.entry("bag", "Bag"), Map.entry("handbag", "Bag"),
            Map.entry("backpack", "Bag"), Map.entry("purse", "Bag"),
            Map.entry("hat", "Hat"), Map.entry("cap", "Hat"),
            Map.entry("scarf", "Scarf"), Map.entry("watch", "Watch"),
            Map.entry("ring", "Ring"), Map.entry("necklace", "Necklace"),
            Map.entry("bracelet", "Bracelet"), Map.entry("earrings", "Earrings")
    );

    private static final Map<String, String> TYPE_CATEGORY = Map.ofEntries(
            Map.entry("jeans", "Bottoms"), Map.entry("trousers", "Bottoms"),
            Map.entry("shorts", "Bottoms"), Map.entry("skirt", "Bottoms"),
            Map.entry("dress", "Dresses"), Map.entry("shirt", "Tops"),
            Map.entry("t-shirt", "Tops"), Map.entry("top", "Tops"),
            Map.entry("sweater", "Tops"), Map.entry("hoodie", "Outerwear"),
            Map.entry("jacket", "Outerwear"), Map.entry("coat", "Outerwear"),
            Map.entry("sneakers", "Shoes"), Map.entry("shoes", "Shoes"),
            Map.entry("boots", "Shoes"), Map.entry("bag", "Bags"),
            Map.entry("hat", "Accessories"), Map.entry("scarf", "Accessories"),
            Map.entry("watch", "Watches"), Map.entry("ring", "Jewelry"),
            Map.entry("necklace", "Jewelry"), Map.entry("bracelet", "Jewelry"),
            Map.entry("earrings", "Jewelry")
    );
}
