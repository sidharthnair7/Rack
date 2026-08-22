package fileidea.rack.integration.serpapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import fileidea.rack.config.SerpApiProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class SerpApiHttpClient implements SerpApiClient {

    private static final Logger log = LoggerFactory.getLogger(SerpApiHttpClient.class);
    private static final int LENS_MAX_BYTES = 500_000;

    private final RestClient http;
    private final JsonMapper mapper;
    private final SerpApiProperties props;
    private final Path cacheDir;

    public SerpApiHttpClient(RestClient http, JsonMapper mapper, SerpApiProperties props) {
        this.http = http;
        this.mapper = mapper;
        this.props = props;
        this.cacheDir = Path.of(props.cacheDir() == null ? "cache/serpapi" : props.cacheDir());
    }

    @Override
    public JsonNode lens(byte[] image) {
        String cacheKey = "lens-" + sha256(image);
        return cached(cacheKey, () -> {
            byte[] body = ImageCompressor.fitUnder(image, LENS_MAX_BYTES);
            String imageId = upload(body);
            return search(uri -> uri
                    .queryParam("engine", "google_lens")
                    .queryParam("image_id", imageId)
                    .queryParam("type", "all"));
        });
    }

    @Override
    public List<ShoppingResult> shopping(String query) {
        JsonNode root = cached("shopping-" + sha256(query.getBytes()), () ->
                search(uri -> uri
                        .queryParam("engine", "google_shopping")
                        .queryParam("q", query)
                        .queryParam("gl", "us")
                        .queryParam("hl", "en")));
        List<ShoppingResult> out = new ArrayList<>();
        JsonNode results = root.path("shopping_results");
        if (results.isArray()) {
            for (JsonNode hit : results) {
                BigDecimal price = decimal(hit, "extracted_price");
                String link = text(hit, "product_link", "link");
                if (price == null || link == null) {
                    continue;
                }
                out.add(new ShoppingResult(price, text(hit, "title"), link));
            }
        }
        return out;
    }

    @Override
    public List<EbaySoldComp> ebaySold(String query) {
        JsonNode root = cached("ebay-" + sha256(query.getBytes()), () ->
                search(uri -> uri
                        .queryParam("engine", "ebay")
                        .queryParam("_nkw", query)
                        .queryParam("ebay_domain", "ebay.com")
                        .queryParam("show_only", "Sold")));
        List<EbaySoldComp> out = new ArrayList<>();
        JsonNode results = root.path("organic_results");
        if (results.isArray()) {
            for (JsonNode hit : results) {
                BigDecimal price = decimal(hit, "extracted_price");
                String link = text(hit, "link");
                if (price == null || link == null) {
                    continue;
                }
                out.add(new EbaySoldComp(
                        text(hit, "title"),
                        price,
                        parseDate(hit),
                        link
                ));
            }
        }
        return out;
    }

    @Override
    public List<Integer> trendSeries(String brandTerm) {
        JsonNode root = cached("trends-" + sha256(brandTerm.getBytes()), () ->
                search(uri -> uri
                        .queryParam("engine", "google_trends")
                        .queryParam("q", brandTerm)
                        .queryParam("data_type", "TIMESERIES")
                        .queryParam("date", "today 12-m")));
        List<Integer> series = new ArrayList<>();
        JsonNode points = root.path("interest_over_time").path("timeline_data");
        if (points.isArray()) {
            for (JsonNode point : points) {
                JsonNode values = point.path("values");
                if (values.isArray() && !values.isEmpty()) {
                    JsonNode extracted = values.get(0).get("extracted_value");
                    if (extracted != null && extracted.isNumber()) {
                        series.add(extracted.intValue());
                    }
                }
            }
        }
        return series;
    }

    private String upload(byte[] image) {
        requireLive("image upload");
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("image", image).filename("item.jpg").contentType(MediaType.IMAGE_JPEG);
        body.part("api_key", props.apiKey());
        String json = http.post()
                .uri("https://serpapi.com/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(String.class);
        JsonNode node = readTree(json);
        String imageId = text(node, "image_id");
        if (imageId == null) {
            throw new IllegalStateException("SerpApi image upload missing image_id: " + json);
        }
        return imageId;
    }

    private JsonNode search(java.util.function.Consumer<UriComponentsBuilder> params) {
        requireLive("search");
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString("https://serpapi.com/search.json")
                .queryParam("api_key", props.apiKey());
        params.accept(uri);
        String json = http.get()
                .uri(uri.encode().build().toUri())
                .retrieve()
                .body(String.class);
        JsonNode node = readTree(json);
        if (node.get("error") != null && !node.get("error").isNull()) {
            throw new IllegalStateException("SerpApi error: " + node.get("error").asString());
        }
        return node;
    }

    private JsonNode cached(String key, java.util.function.Supplier<JsonNode> fetch) {
        Path file = cacheDir.resolve(key + ".json");
        if (Files.exists(file)) {
            log.debug("serpapi cache hit {}", key);
            return readFile(file);
        }
        JsonNode fresh = fetch.get();
        try {
            Files.createDirectories(cacheDir);
            Files.writeString(file, mapper.writeValueAsString(fresh));
        } catch (IOException e) {
            log.warn("could not write serpapi cache {}", file, e);
        }
        return fresh;
    }

    private void requireLive(String what) {
        if (props.cacheOnly() || !props.hasKey()) {
            throw new IllegalStateException(
                    "SerpApi cache miss for " + what + ". Set rack.serpapi.api-key (and cache-only=false) or drop a cached JSON under "
                            + cacheDir.toAbsolutePath()
            );
        }
    }

    private JsonNode readFile(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode readTree(String json) {
        return mapper.readTree(json == null ? "{}" : json);
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asString().isBlank()) {
                return value.asString();
            }
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
            String raw = value.asString().replaceAll("[^0-9.]", "");
        if (raw.isBlank()) {
            return null;
        }
        return new BigDecimal(raw);
    }

    private static Instant parseDate(JsonNode hit) {
        String raw = text(hit, "date", "sold_date");
        if (raw == null) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
