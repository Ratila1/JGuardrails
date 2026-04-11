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
 * OpenAI-compatible LLM client using {@link java.net.http.HttpClient}.
 *
 * <p>Compatible with: OpenAI, Azure OpenAI, vLLM, LiteLLM, and any OpenAI-compatible API.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * LlmClient client = OpenAiClient.builder()
 *     .apiKey(System.getenv("OPENAI_API_KEY"))
 *     .model("gpt-4o-mini")
 *     .build();
 * }</pre>
 */
public class OpenAiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final HttpClient httpClient;

    private OpenAiClient(Builder builder) {
        this.apiKey = Objects.requireNonNull(builder.apiKey, "apiKey must not be null");
        this.baseUrl = builder.baseUrl.endsWith("/")
                ? builder.baseUrl.substring(0, builder.baseUrl.length() - 1)
                : builder.baseUrl;
        this.defaultModel = builder.model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(builder.timeoutMs))
                .build();
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        try {
            String body = buildRequestBody(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new GuardrailException(
                        "OpenAI API error: HTTP " + response.statusCode() + " — " + response.body());
            }
            return parseResponse(response.body());
        } catch (GuardrailException e) {
            throw e;
        } catch (Exception e) {
            throw new GuardrailException("Failed to call OpenAI API", e);
        }
    }

    @Override
    public CompletableFuture<LlmResponse> chatAsync(LlmRequest request) {
        try {
            String body = buildRequestBody(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new GuardrailException(
                                    "OpenAI API error: HTTP " + response.statusCode());
                        }
                        return parseResponse(response.body());
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                    new GuardrailException("Failed to build OpenAI request", e));
        }
    }

    private String buildRequestBody(LlmRequest request) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("model", defaultModel);
            root.put("temperature", request.getTemperature());
            root.put("max_tokens", request.getMaxTokens());

            ArrayNode messages = root.putArray("messages");
            for (LlmRequest.Message msg : request.getMessages()) {
                ObjectNode msgNode = messages.addObject();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
            }
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new GuardrailException("Failed to serialize OpenAI request", e);
        }
    }

    private LlmResponse parseResponse(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            String content = root.path("choices").path(0)
                    .path("message").path("content").asText();
            String model = root.path("model").asText("unknown");
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            return new LlmResponse(content, model, promptTokens, completionTokens);
        } catch (Exception e) {
            throw new GuardrailException("Failed to parse OpenAI response", e);
        }
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link OpenAiClient}. */
    public static final class Builder {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private int timeoutMs = 5000;

        private Builder() {}

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }

        public OpenAiClient build() { return new OpenAiClient(this); }
    }
}
