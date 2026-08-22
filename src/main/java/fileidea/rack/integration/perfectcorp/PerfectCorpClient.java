package fileidea.rack.integration.perfectcorp;

import java.util.Map;

public interface PerfectCorpClient {

    String upload(String feature, byte[] image, String filename);

    String submit(String service, Map<String, Object> body);

    TaskResult poll(String service, String taskId);

    byte[] download(String url);

    record TaskResult(String status, String resultUrl, String dstId, Integer pollingIntervalSec) {
        public boolean running() {
            return "running".equalsIgnoreCase(status);
        }

        public boolean success() {
            return "success".equalsIgnoreCase(status);
        }
    }
}
