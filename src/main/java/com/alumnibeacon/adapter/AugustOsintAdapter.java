package com.alumnibeacon.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class AugustOsintAdapter {

    private final WebClient webClient;

    public AugustOsintAdapter(@Value("${august.adapter.url}") String adapterUrl,
                               @Value("${august.adapter.timeout}") long timeoutMs) {
        this.webClient = WebClient.builder()
            .baseUrl(adapterUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    public Mono<String> search(String payloadJson) {
        log.info("Sending OSINT search to August adapter");
        return webClient.post()
            .uri("/osint/search")
            .bodyValue(payloadJson)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(120))
            .doOnSuccess(r -> log.info("August adapter returned result"))
            .doOnError(e -> log.error("August adapter error: {}", e.getMessage()));
    }

    public Mono<Map> healthCheck() {
        return webClient.get()
            .uri("/health")
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(5));
    }
}
