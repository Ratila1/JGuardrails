package io.jguardrails.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jguardrails.core.GuardrailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Ollama LLM client for local model inference.
 *
 * <p>Connects to a locally running Ollama instance (default: {@code http://localhost:11434}).
 * Enables LLM-as-judge guardrails without any cloud API dependencies.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * LlmClient client = OllamaClient.builder()
 *     .model("llama3")
 *     .build();
 * }</pre>
 */
public class OllamaClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;

    private OllamaClient(Builder builder) {
        this.baseUrl = builder.baseUrl.endsWith("/")
                ? builder.baseUrl.substring(0, builder.baseUrl.length() - 1)
                : builder.baseUrl;
        this.model = Objects.requireNonNull(builder.model, "model must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        try {
            String body = buildRequestBody(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new GuardrailException(
                        "Ollama API error: HTTP " + response.statusCode() + " — " + response.body());
            }
            return parseResponse(response.body());
        } catch (GuardrailException e) {
            throw e;
        } catch (Exception e) {
            throw new GuardrailException("Failed to call Ollama API", e);
        }
    }

    @Override
    public CompletableFuture<LlmResponse> chatAsync(LlmRequest request) {
        try {
            String body = buildRequestBody(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new GuardrailException(
                                    "Ollama API error: HTTP " + response.statusCode());
                        }
                        return parseResponse(response.body());
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                    new GuardrailException("Failed to build Ollama request", e));
        }
    }

    private String buildRequestBody(LlmRequest request) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("model", model);
            root.put("stream", false);

            ArrayNode messages = root.putArray("messages");
            for (LlmRequest.Message msg : request.getMessages()) {
                ObjectNode msgNode = messages.addObject();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
            }
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new GuardrailException("Failed to serialize Ollama request", e);
        }
    }

    private LlmResponse parseResponse(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            String content = root.path("message").path("content").asText();
            return new LlmResponse(content, model, 0, 0);
        } catch (Exception e) {
            throw new GuardrailException("Failed to parse Ollama response", e);
        }
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link OllamaClient}. */
    public static final class Builder {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3";

        private Builder() {}

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder model(String model) { this.model = model; return this; }

        public OllamaClient build() { return new OllamaClient(this); }
    }
}
