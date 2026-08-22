package fileidea.rack.integration.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalImageStorage implements ImageStorage {

    private static final String PUBLIC_PREFIX = "/uploads/";

    private final Path root;

    public LocalImageStorage(@Value("${rack.storage.local-dir:uploads}") String dir) {
        this.root = Path.of(dir);
    }

    @Override
    public String store(String key, byte[] bytes, String contentType) {
        try {
            Path rootAbs = root.toAbsolutePath().normalize();
            Path target = rootAbs.resolve(key).normalize();
            if (!target.startsWith(rootAbs)) {
                throw new IllegalArgumentException("invalid storage key");
            }
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return publicUrl(key);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] read(String key) {
        try {
            Path rootAbs = root.toAbsolutePath().normalize();
            Path target = rootAbs.resolve(key).normalize();
            if (!target.startsWith(rootAbs)) {
                throw new IllegalArgumentException("invalid storage key");
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String publicUrl(String key) {
        return PUBLIC_PREFIX + key.replace('\\', '/');
    }

    @Override
    public String keyFromPublicUrl(String publicUrl) {
        if (publicUrl != null && publicUrl.startsWith(PUBLIC_PREFIX)) {
            return publicUrl.substring(PUBLIC_PREFIX.length());
        }
        return publicUrl;
    }
}
