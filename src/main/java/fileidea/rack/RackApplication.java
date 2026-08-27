package fileidea.rack;

import fileidea.rack.config.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class RackApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RackApplication.class);
        // Wired directly, not via an SPI file: see Dotenv's Javadoc for why that matters here.
        app.addInitializers(Dotenv::apply);
        app.run(args);
    }

}
