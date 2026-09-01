package fileidea.rack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String storageDir;

    public WebConfig(@Value("${rack.storage.local-dir:uploads}") String storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Any local port, not a fixed list. Vite moves to 5174, 5175, ... whenever its preferred
        // port is already taken, and a hardcoded 5173 turns that silent port hop into a
        // "403 Invalid CORS request" that looks like the backend is down. This only ever matches
        // loopback origins, and in production the UI is served from this same origin anyway, so
        // no cross-origin request happens at all.
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + storageDir + "/");
    }
}
