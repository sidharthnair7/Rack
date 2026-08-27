package fileidea.rack.config;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a {@code .env} file from the working directory into the Spring Environment, so
 * {@code RACK_SERPAPI_API_KEY=...} in {@code .env} binds exactly like an exported shell
 * variable — drop a key in, restart, done.
 *
 * <p>Wired directly in {@code RackApplication.main()} via
 * {@code SpringApplication.addInitializers(Dotenv::apply)} — explicit code, not SPI discovery.
 * That is the load-bearing decision here, made only after two SPI-based attempts were each
 * built, booted, and proven not to run via a throwaway probe bean that printed the resolved
 * {@link org.springframework.core.env.Environment}:
 * <ol>
 *   <li>{@code me.paulschwarz:spring-dotenv} registers via the legacy
 *       {@code META-INF/spring.factories} mechanism (last published 2023) and never loaded
 *       under Spring Boot 4.1.1 — no {@code dotenv} property source ever appeared.</li>
 *   <li>A hand-written {@code EnvironmentPostProcessor}, registered via the current
 *       {@code .imports} file, also never ran — that interface itself is deprecated for removal
 *       in this Boot version; the compiler warning confirming it was in the build output the
 *       whole time and got missed until the probe forced a closer look.</li>
 * </ol>
 * An {@code ApplicationContextInitializer} added by direct method reference has no discovery
 * step to silently skip — if {@code main()} calls it, it runs.
 *
 * <p>The property source is a {@link SystemEnvironmentPropertySource}, not a plain
 * {@code MapPropertySource}: that specific class is what makes a stored key like
 * {@code RACK_SERPAPI_API_KEY} resolve when something asks for {@code rack.serpapi.api-key} —
 * the same translation a real OS environment variable relies on. A plain map property source
 * does not perform it.
 *
 * <p>Precedence is enforced explicitly: inserted immediately after {@code systemEnvironment}, so
 * a real OS environment variable always wins over {@code .env}, which always wins over an
 * {@code application.properties} default. A real deployment target has no {@code .env} file, so
 * this is a no-op there regardless of precedence.
 */
public final class Dotenv {

    private static final String SOURCE_NAME = "dotenvFile";

    private Dotenv() {
    }

    public static void apply(ConfigurableApplicationContext context) {
        apply(context, Path.of(".env"));
    }

    /**
     * Package-private seam for tests: {@code Path.of(".env")} resolves against the JDK's default
     * {@code FileSystemProvider}, which caches the JVM's working directory once at startup and
     * does not observe later changes to the {@code user.dir} system property — so a test cannot
     * redirect the real {@link #apply(ConfigurableApplicationContext)} at an arbitrary temp
     * directory by changing {@code user.dir} at runtime. Passing the path explicitly sidesteps
     * that JDK behaviour instead of fighting it.
     */
    static void apply(ConfigurableApplicationContext context, Path envFile) {
        if (!Files.isRegularFile(envFile)) {
            return;
        }
        Map<String, Object> values = parse(envFile);
        if (values.isEmpty()) {
            return;
        }
        PropertySource<?> source = new SystemEnvironmentPropertySource(SOURCE_NAME, values);
        var sources = context.getEnvironment().getPropertySources();
        if (sources.contains("systemEnvironment")) {
            sources.addAfter("systemEnvironment", source);
        } else {
            sources.addFirst(source);
        }
    }

    private static Map<String, Object> parse(Path envFile) {
        Map<String, Object> values = new LinkedHashMap<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return values;
        }
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).strip();
            String value = unquote(line.substring(eq + 1).strip());
            if (!value.isBlank()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
