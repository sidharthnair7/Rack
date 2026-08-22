package fileidea.rack.integration.namecom;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fileidea.rack.config.NameComProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NameComHttpClient implements NameComClient {

    private final RestClient http;
    private final JsonMapper mapper;
    private final NameComProperties props;

    public NameComHttpClient(RestClient http, JsonMapper mapper, NameComProperties props) {
        this.http = http;
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public List<String> search(String query) {
        requireCreds();
        JsonNode root = post("/core/v1/domains:search", Map.of(
                "keyword", query,
                "tldFilter", List.of("com", "store", "shop"),
                "purchaseType", "registration"
        ));
        List<String> names = new ArrayList<>();
        for (JsonNode hit : iterable(root.path("results"))) {
            String name = text(hit, "domainName");
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    @Override
    public boolean available(String domain) {
        requireCreds();
        JsonNode root = post("/core/v1/domains:checkAvailability", Map.of("domainNames", List.of(domain)));
        JsonNode results = root.path("results");
        if (results.isArray() && !results.isEmpty()) {
            JsonNode first = results.get(0);
            JsonNode purchasable = first.get("purchasable");
            if (purchasable != null && !purchasable.isNull()) {
                return purchasable.booleanValue();
            }
        }
        return false;
    }

    @Override
    public void register(String domain) {
        requireCreds();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("domain", Map.of("domainName", domain));
        body.put("years", 1);
        post("/core/v1/domains", body, Map.of("X-Idempotency-Key", UUID.randomUUID().toString()));
    }

    @Override
    public void createDnsRecord(String domain, String host, String type, String answer) {
        requireCreds();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", host == null ? "" : host);
        body.put("type", type);
        body.put("answer", answer);
        body.put("ttl", 300);
        post("/core/v1/domains/" + domain + "/records", body);
    }

    @Override
    public void createSubdomain(String domain, String host) {
        if (props.storefrontIp() == null || props.storefrontIp().isBlank()) {
            return;
        }
        createDnsRecord(domain, host, "A", props.storefrontIp());
    }

    @Override
    public void createUrlForward(String domain, String host, String destination) {
        requireCreds();
        post("/core/v1/domains/" + domain + "/urlForwardings", Map.of(
                "host", host == null ? "" : host,
                "forwardsTo", destination
        ));
    }

    public boolean enabled() {
        return props.hasCreds();
    }

    private void requireCreds() {
        if (!props.hasCreds()) {
            throw new IllegalStateException("rack.namecom.username / token are empty");
        }
    }

    private JsonNode post(String path, Map<String, Object> body) {
        return post(path, body, Map.of());
    }

    private JsonNode post(String path, Map<String, Object> body, Map<String, String> extraHeaders) {
        var spec = http.post()
                .uri(props.baseUrl() + path)
                .header("Authorization", basic())
                .contentType(MediaType.APPLICATION_JSON);
        extraHeaders.forEach(spec::header);
        String json = spec.body(mapper.writeValueAsString(body)).retrieve().body(String.class);
        return mapper.readTree(json == null ? "{}" : json);
    }

    private String basic() {
        String raw = props.username() + ":" + props.token();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<JsonNode> out = new ArrayList<>();
        node.forEach(out::add);
        return out;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asString().isBlank()) {
            return null;
        }
        return value.asString();
    }
}
