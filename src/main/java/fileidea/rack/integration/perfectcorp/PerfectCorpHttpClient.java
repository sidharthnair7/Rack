package fileidea.rack.integration.perfectcorp;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fileidea.rack.config.PerfectCorpProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class PerfectCorpHttpClient implements PerfectCorpClient {

    private final RestClient http;
    private final JsonMapper mapper;
    private final PerfectCorpProperties props;

    public PerfectCorpHttpClient(RestClient http, JsonMapper mapper, PerfectCorpProperties props) {
        this.http = http;
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public String upload(String feature, byte[] image, String filename) {
        requireKey();
        String contentType = filename.toLowerCase().endsWith(".png") ? "image/png" : "image/jpg";
        Map<String, Object> file = new HashMap<>();
        file.put("content_type", contentType);
        file.put("file_name", filename);
        file.put("file_size", image.length);
        Map<String, Object> body = Map.of("files", java.util.List.of(file));

        JsonNode root = postJson(props.baseUrl() + "/s2s/v2.0/file/" + feature, body);
        JsonNode uploaded = root.path("data").path("files").path(0);
        String fileId = text(uploaded, "file_id");
        JsonNode request = uploaded.path("requests").path(0);
        String url = text(request, "url");
        if (fileId == null || url == null) {
            throw new IllegalStateException("Perfect Corp upload missing file_id/url: " + root);
        }
        // URI.create, not the String overload: RestClient treats a String uri as a template and
        // re-encodes it, which mangles the percent-encoded slashes inside a presigned S3 URL's
        // X-Amz-Credential and gets every upload rejected with AuthorizationQueryParametersError.
        // Passing an already-parsed URI tells RestClient the URL is final - do not touch it.
        RestClient.RequestBodySpec put = http.put().uri(URI.create(url));
        JsonNode headers = request.path("headers");
        if (headers instanceof tools.jackson.databind.node.ObjectNode objectNode) {
            objectNode.properties().forEach(e -> put.header(e.getKey(), e.getValue().asString()));
        }
        put.body(image).retrieve().toBodilessEntity();
        return fileId;
    }

    @Override
    public String submit(String service, Map<String, Object> body) {
        requireKey();
        JsonNode root = postJson(props.baseUrl() + "/s2s/v2.0/task/" + service, body);
        String taskId = text(root.path("data"), "task_id");
        if (taskId == null) {
            throw new IllegalStateException("Perfect Corp submit missing task_id: " + root);
        }
        return taskId;
    }

    @Override
    public TaskResult poll(String service, String taskId) {
        requireKey();
        String json = http.get()
                .uri(props.baseUrl() + "/s2s/v2.0/task/" + service + "/" + taskId)
                .header("Authorization", "Bearer " + props.apiKey())
                .retrieve()
                .body(String.class);
        JsonNode root = mapper.readTree(json == null ? "{}" : json);
        JsonNode data = root.path("data");
        String status = text(data, "task_status");
        String url = text(data.path("results"), "url");
        String dstId = text(data, "dst_id");
        Integer interval = data.get("polling_interval") != null && data.get("polling_interval").isNumber()
                ? data.get("polling_interval").intValue()
                : 15;
        return new TaskResult(status, url, dstId, interval);
    }

    @Override
    public byte[] download(String url) {
        // Same presigned-URL hazard as upload(): never let RestClient re-encode a result URL.
        byte[] bytes = http.get().uri(URI.create(url)).retrieve().body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("empty download from " + url);
        }
        return bytes;
    }

    private JsonNode postJson(String url, Map<String, Object> body) {
        String json = http.post()
                .uri(url)
                .header("Authorization", "Bearer " + props.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);
        return mapper.readTree(json == null ? "{}" : json);
    }

    private void requireKey() {
        if (!props.hasKey()) {
            throw new IllegalStateException("rack.perfectcorp.api-key is empty");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asString().isBlank()) {
            return null;
        }
        return value.asString();
    }
}
