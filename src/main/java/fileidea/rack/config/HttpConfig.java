package fileidea.rack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpConfig {

    @Bean
    RestClient restClient() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(90));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
