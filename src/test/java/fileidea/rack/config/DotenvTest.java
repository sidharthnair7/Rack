package fileidea.rack.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Exercises the actual failure this class exists to prevent: a value in .env silently not
 * reaching the code that needs it. Two earlier real attempts (a third-party library, then a
 * hand-written EnvironmentPostProcessor) both compiled cleanly and both silently did nothing —
 * so this boots a real minimal Spring context against a real file on disk and asserts on what
 * the Environment resolves, rather than on Dotenv's internals, which is exactly the level
 * neither prior attempt was wrong at.
 */
class DotenvTest {

    // Deliberately a bare @Configuration, not @SpringBootApplication: this class lives in
    // fileidea.rack.config, so component-scanning that package would drag in the real app's
    // beans (DemoData -> SellerRepository -> the full JPA stack) for a test that only needs to
    // check what one property resolves to.
    @Configuration
    static class Probe {
    }

    private static ConfigurableApplicationContext boot(Path envFile) {
        SpringApplication app = new SpringApplication(Probe.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.addInitializers(context -> Dotenv.apply(context, envFile));
        return app.run();
    }

    @Test
    void aKeyInDotEnvResolvesInTheSpringDottedFormItsPropertiesRecordsExpect(@TempDir Path tempDir)
            throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "RACK_SERPAPI_API_KEY=probe_value_123\n");

        try (var ctx = boot(envFile)) {
            assertEquals("probe_value_123", ctx.getEnvironment().getProperty("rack.serpapi.api-key"));
        }
    }

    @Test
    void quotedAndCommentedAndBlankLinesAreHandled(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
                # a comment, ignored
                RACK_STRIPE_SECRET_KEY="sk_test_quoted"
                RACK_NAMECOM_USERNAME='single_quoted'

                RACK_NAMECOM_TOKEN=
                not a valid line at all
                """);

        try (var ctx = boot(envFile)) {
            assertEquals("sk_test_quoted", ctx.getEnvironment().getProperty("rack.stripe.secret-key"));
            assertEquals("single_quoted", ctx.getEnvironment().getProperty("rack.namecom.username"));
            assertNull(ctx.getEnvironment().getProperty("rack.namecom.token"),
                    "a blank value must not shadow a real default with an empty string");
        }
    }

    @Test
    void aRealEnvironmentVariableWinsOverDotEnv(@TempDir Path tempDir) throws IOException {
        // Cannot set a real OS env var from within the JVM, so this asserts the documented
        // contract at the unit level instead: any key System.getenv() already has must be
        // skipped entirely rather than added to the dotenv property source.
        String realKey = System.getenv().keySet().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("test environment has no env vars to use"));
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, realKey + "=this_must_never_be_used\n");

        try (var ctx = boot(envFile)) {
            assertEquals(System.getenv(realKey), ctx.getEnvironment().getProperty(realKey),
                    "a real environment variable must never be shadowed by .env");
        }
    }

    @Test
    void aMissingDotEnvFileIsNotAnError(@TempDir Path tempDir) {
        try (var ctx = boot(tempDir.resolve("does-not-exist.env"))) {
            assertNull(ctx.getEnvironment().getProperty("rack.serpapi.api-key"));
        }
    }
}
