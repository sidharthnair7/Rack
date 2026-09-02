package fileidea.rack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class HttpConfig {

    /**
     * Fan-out for vendor calls that do not depend on each other.
     *
     * <p>Pricing asks SerpApi three separate questions per garment (eBay comparables, retail price
     * new, demand trend). Run in sequence they cost the sum of three network round trips while the
     * seller watches a spinner; nothing in the second call needs the first one's answer.
     *
     * <p>Virtual threads because this is blocking IO with no CPU work worth pooling for: each task
     * spends its life parked on a socket, so the carrier thread is released rather than held.
     */
    @Bean(destroyMethod = "close")
    ExecutorService vendorFanout() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

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
