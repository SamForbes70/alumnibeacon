package com.alumnibeacon.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class AugustOsintAdapter {

    private final WebClient webClient;
    private final long timeoutMs;

    public AugustOsintAdapter(@Value("${august.adapter.url}") String adapterUrl,
                               @Value("${august.adapter.timeout}") long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.webClient = WebClient.builder()
            .baseUrl(adapterUrl)
            .build();
        log.info("AugustOsintAdapter configured: url={}, timeout={}ms", adapterUrl, timeoutMs);
    }

    public Mono<String> search(String payloadJson) {
        log.info("Sending OSINT search to August adapter (payload length={})", payloadJson.length());
        return webClient.post()
            .uri("/osint/search")
            .contentType(MediaType.APPLICATION_JSON)   // ← explicit JSON content type
            .bodyValue(payloadJson)                     // ← raw JSON string
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError() || status.is5xxServerError(),
                response -> response.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Adapter error {}: {}", response.statusCode(), body);
                        return Mono.error(new RuntimeException(
                            response.statusCode() + " from adapter: " + body));
                    })
            )
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .doOnSuccess(r -> log.info("August adapter returned result (length={})", r != null ? r.length() : 0))
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
