package fileidea.rack;

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
        SpringApplication.run(RackApplication.class, args);
    }

}
