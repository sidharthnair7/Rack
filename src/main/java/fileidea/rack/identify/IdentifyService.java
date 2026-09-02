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
import java.util.Comparator;
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
     *
     * <p>Frequency alone is not enough, and the failure is not subtle: a pair of jeans came back
     * branded <em>Solid</em>, because colour and fit words recur across independent listings in
     * exactly the way a brand does. That wrong word then becomes the eBay query, so it takes the
     * comps, the median and the listing title down with it.
     *
     * <p>So position breaks the tie. Marketplace titles are written brand-first - "Levi's Men's
     * Slim Straight Jeans", "American Eagle Outfitters Men's Blue Dark Wash Denim" - while the
     * descriptors trail behind. A token still has to appear in at least two independent titles to
     * be considered at all; among those that do, the one that sits earliest wins.
     */
    static String recurringBrand(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        String known = knownBrandIn(titles);
        if (known != null) {
            return known;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Double> leadingness = new LinkedHashMap<>();
        Map<String, String> display = new LinkedHashMap<>();
        for (String title : titles) {
            Set<String> seenInThisTitle = new HashSet<>();
            int position = 0;
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
                    // Decaying weight: first surviving token in a title scores 1, the next 1/2,
                    // then 1/3. Summed across titles, a word that consistently leads outranks one
                    // that merely appears often.
                    leadingness.merge(key, 1.0 / (1 + position), Double::sum);
                    display.putIfAbsent(key, word);
                }
                position++;
            }
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .max(Comparator.comparingDouble(entry -> leadingness.getOrDefault(entry.getKey(), 0.0)))
                .map(top -> display.get(top.getKey()))
                .orElseGet(() -> display.values().stream().findFirst().orElse(null));
    }

    /**
     * Look for a brand we actually recognise before falling back to counting words.
     *
     * <p>Token frequency is a guess, and the guesses fail in a way that is obvious to anyone
     * looking: a pair of jeans was branded "Solid", and once that word was blocked the same photo
     * came back "Long". Adding another word to the blocklist each time is whack-a-mole, because
     * the underlying signal - "this word appears a lot" - genuinely cannot distinguish a brand
     * from an adjective.
     *
     * <p>A name from this list is different in kind: if "Levi's" appears across independent
     * listings of the same garment, that is not a coincidence to be weighed, it is the answer.
     * Longest first, so "American Eagle" is not shortened to "Eagle" and "North Face" beats
     * "Face". The heuristic still runs for anything unlisted, and the brand stays user-editable,
     * because no fixed list covers secondhand clothing.
     */
    static String knownBrandIn(List<String> titles) {
        String best = null;
        int bestHits = 0;
        for (String brand : KNOWN_BRANDS) {
            String needle = brand.toLowerCase(Locale.ROOT);
            int hits = 0;
            for (String title : titles) {
                if (title != null && title.toLowerCase(Locale.ROOT).contains(needle)) {
                    hits++;
                }
            }
            // A brand named in two independent listings of the same garment is the brand. One
            // mention is as likely to be a "similar to" suggestion in a marketplace title.
            if (hits >= 2 && hits > bestHits) {
                best = brand;
                bestHits = hits;
            }
        }
        return best;
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

    /**
     * Apparel brands common in secondhand resale, longest first so multi-word names win over the
     * single words inside them. Not exhaustive by design - it is a precision filter, not a
     * catalogue, and anything it misses falls through to the frequency heuristic.
     */
    private static final List<String> KNOWN_BRANDS = List.of(
            "American Eagle", "Abercrombie & Fitch", "Abercrombie", "Tommy Hilfiger",
            "Ralph Lauren", "Calvin Klein", "Under Armour", "The North Face", "North Face",
            "Canada Goose", "Fruit of the Loom", "Banana Republic", "Urban Outfitters",
            "Brooks Brothers", "Marc Jacobs", "Michael Kors", "Kate Spade", "Stone Island",
            "Fred Perry", "Ben Sherman", "Paul Smith", "Hugo Boss", "Emporio Armani", "Armani",
            "Dolce & Gabbana", "Yves Saint Laurent", "Saint Laurent", "Louis Vuitton",
            "Bottega Veneta", "Salvatore Ferragamo", "Alexander McQueen", "Vivienne Westwood",
            "Comme des Garcons", "Acne Studios", "Norse Projects", "Massimo Dutti",
            "Sport Chek", "Eddie Bauer", "L.L.Bean", "Duluth Trading", "Helly Hansen",
            "Mountain Hardwear", "Black Diamond", "Fjallraven", "Sweaty Betty", "Free People",
            "Anthropologie", "Forever 21", "Charlotte Russe", "Old Navy", "New Balance",
            "Levi's", "Levis", "Nike", "Adidas", "Puma", "Reebok", "Converse", "Vans",
            "Carhartt", "Champion", "Patagonia", "Columbia", "Arcteryx", "Moncler", "Burberry",
            "Gucci", "Prada", "Versace", "Balenciaga", "Givenchy", "Fendi", "Hermes", "Chanel",
            "Dior", "Lacoste", "Superdry", "Diesel", "Wrangler", "Dickies", "Timberland",
            "Uniqlo", "Zara", "Mango", "Bershka", "Primark", "Topshop", "Reiss", "Whistles",
            "Lululemon", "Athleta", "Gymshark", "Fabletics", "Everlane", "Madewell", "Talbots",
            "Aritzia", "Roots", "Guess", "Hollister", "Gildan", "Hanes", "Wilson", "Oakley",
            "Quiksilver", "Billabong", "Rip Curl", "Volcom", "Element", "Thrasher", "Supreme",
            "Stussy", "Obey", "Herschel", "Jansport", "Osprey", "Salomon", "Merrell", "Keen",
            "Clarks", "Doc Martens", "Dr. Martens", "Birkenstock", "Crocs", "Skechers", "Asics",
            "Brooks", "Saucony", "Hoka", "Fila", "Kappa", "Umbro", "Ellesse", "Sergio Tacchini",
            "Jordan", "Yeezy", "Bape", "Palace", "Carhartt WIP", "Gap", "Uniqlo U", "Muji",
            "Cos", "Arket", "Weekday", "Monki", "Nordstrom", "Macys", "Kohls", "Target",
            "Walmart", "Costco", "Lands End", "J.Crew", "JCrew", "Express", "Aeropostale",
            "Pacsun", "Zumiez", "Boohoo", "Shein", "Asos", "Reformation", "Sezane", "Ganni"
    );

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
            "isolated", "background", "closeup", "close", "top", "view", "flat", "lay", "mockup",
            // Colour, fabric, fit and construction words. These recur across independent listings
            // in exactly the way a brand does, which is how a pair of jeans came back branded
            // "Solid". Position weighting in recurringBrand handles the rest; this removes the
            // ones frequent enough to lead a title on their own ("Blue Drawstring Jeans").
            "solid", "wash", "washed", "dark", "cotton", "wool", "linen", "leather", "suede",
            "fleece", "nylon", "polyester", "cashmere", "silk", "corduroy", "flannel", "red",
            "pink", "purple", "yellow", "orange", "cream", "khaki", "olive", "burgundy",
            "maroon", "teal", "ivory", "charcoal", "striped", "stripe", "plaid", "checked",
            "floral", "graphic", "print", "printed", "patterned", "distressed", "faded",
            "cropped", "oversized", "fitted", "loose", "baggy", "tapered", "bootcut", "skinny",
            "rise", "waist", "sleeve", "sleeved", "zip", "zipper", "button", "buttoned",
            "collar", "collared", "hooded", "drawstring", "lined", "unlined", "pocket",
            "pockets", "logo", "accent", "series", "edition", "casual", "formal", "everyday",
            "soft", "heavy", "lightweight", "adult", "youth", "petite", "excellent", "clean"
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
