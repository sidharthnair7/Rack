package fileidea.rack.integration.storage;

public interface ImageStorage {

    String store(String key, byte[] bytes, String contentType);

    byte[] read(String key);

    String publicUrl(String key);

    String keyFromPublicUrl(String publicUrl);
}
