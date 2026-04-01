package com.alumnibeacon.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Routes OSINT investigations to the appropriate engine.
 *
 * Engine resolution order (highest to lowest priority):
 *   1. Per-investigation preferredEngine (set by user on the form)
 *   2. Global osint.engine config (application.properties / env var)
 *
 * Modes:
 *   python      - calls Python FastAPI adapter /osint/search (~60s)
 *   agent-zero  - calls Agent Zero A2A bridge /agent-zero/investigate (~15min)
 *   hybrid      - tries agent-zero first, falls back to python on failure
 */
@Component
@Slf4j
public class OsintAdapterRouter {

    private final WebClient adapterClient;
    private final String globalEngine;
    private final long pythonTimeoutMs;
    private final long agentZeroTimeoutMs;
    private final ObjectMapper objectMapper;

    public OsintAdapterRouter(
            @Value("${august.adapter.url:http://localhost:8000}") String adapterUrl,
            @Value("${osint.engine:python}") String globalEngine,
            @Value("${august.adapter.timeout:120000}") long pythonTimeoutMs,
            @Value("${agent-zero.timeout:900000}") long agentZeroTimeoutMs,
            ObjectMapper objectMapper) {
        this.globalEngine = globalEngine.toLowerCase().trim();
        this.pythonTimeoutMs = pythonTimeoutMs;
        this.agentZeroTimeoutMs = agentZeroTimeoutMs;
        this.objectMapper = objectMapper;
        this.adapterClient = WebClient.builder()
            .baseUrl(adapterUrl)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
        log.info("OsintAdapterRouter configured: globalEngine={}, adapterUrl={}, pythonTimeout={}ms, agentZeroTimeout={}ms",
            this.globalEngine, adapterUrl, pythonTimeoutMs, agentZeroTimeoutMs);
    }

    /**
     * Route using global engine config (backward-compatible overload).
     */
    public Mono<String> route(String payloadJson) {
        return route(payloadJson, null);
    }

    /**
     * Route with per-investigation engine override.
     * If preferredEngine is non-null and non-blank, it takes priority over global config.
     */
    public Mono<String> route(String payloadJson, String preferredEngine) {
        String engine = resolveEngine(preferredEngine);
        log.info("Routing investigation: preferredEngine={}, globalEngine={}, resolved={}",
            preferredEngine, globalEngine, engine);

        return switch (engine) {
            case "agent-zero" -> callAgentZero(payloadJson)
                .map(json -> injectEngine(json, "agent-zero"));
            case "hybrid" -> callAgentZero(payloadJson)
                .map(json -> injectEngine(json, "agent-zero"))
                .onErrorResume(e -> {
                    log.warn("Agent Zero failed ({}), falling back to Python adapter", e.getMessage());
                    return callPython(payloadJson)
                        .map(json -> injectEngine(json, "python-fallback"));
                });
            default -> callPython(payloadJson)
                .map(json -> injectEngine(json, "python"));
        };
    }

    /**
     * Resolve the effective engine: per-investigation preference takes priority over global config.
     */
    private String resolveEngine(String preferredEngine) {
        if (preferredEngine != null && !preferredEngine.isBlank()) {
            String pe = preferredEngine.trim().toLowerCase();
            // Only allow known engine values
            if (pe.equals("agent-zero") || pe.equals("python") || pe.equals("hybrid")) {
                return pe;
            }
        }
        return globalEngine;
    }

    /** Call the Python FastAPI adapter at /osint/search */
    private Mono<String> callPython(String payloadJson) {
        log.info("Calling Python adapter");
        return adapterClient.post()
            .uri("/osint/search")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payloadJson)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError() || status.is5xxServerError(),
                response -> response.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Python adapter error {}: {}", response.statusCode(), body);
                        return Mono.error(new RuntimeException(
                            response.statusCode() + " from Python adapter: " + body));
                    })
            )
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(pythonTimeoutMs))
            .doOnSuccess(r -> log.info("Python adapter returned result (length={})", r != null ? r.length() : 0))
            .doOnError(e -> log.error("Python adapter error: {}", e.getMessage()));
    }

    /** Call the Agent Zero A2A bridge at /agent-zero/investigate */
    private Mono<String> callAgentZero(String payloadJson) {
        log.info("Calling Agent Zero A2A bridge");
        return adapterClient.post()
            .uri("/agent-zero/investigate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payloadJson)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError() || status.is5xxServerError(),
                response -> response.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Agent Zero bridge error {}: {}", response.statusCode(), body);
                        return Mono.error(new RuntimeException(
                            response.statusCode() + " from Agent Zero bridge: " + body));
                    })
            )
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(agentZeroTimeoutMs))
            .doOnSuccess(r -> log.info("Agent Zero bridge returned result (length={})", r != null ? r.length() : 0))
            .doOnError(e -> log.error("Agent Zero bridge error: {}", e.getMessage()));
    }

    /** Inject the 'engine' field into the result JSON string. */
    private String injectEngine(String json, String engine) {
        if (json == null || json.isBlank()) return json;
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(json);
            node.put("engine", engine);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Could not inject engine field into result JSON: {}", e.getMessage());
            return json;
        }
    }

    public String getGlobalEngine() { return globalEngine; }
}
