package io.jguardrails.detectors.output.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Validates that LLM responses are valid JSON.
 *
 * <p>Useful when the LLM is prompted to return structured data.
 * Blocks responses that are not parseable as JSON.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * JsonSchemaValidator validator = JsonSchemaValidator.builder()
 *     .requireValidJson(true)
 *     .build();
 * }</pre>
 */
public class JsonSchemaValidator implements OutputRail {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final boolean requireValidJson;

    private JsonSchemaValidator(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.requireValidJson = builder.requireValidJson;
    }

    @Override
    public String name() { return name; }

    @Override
    public int priority() { return priority; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public RailResult process(String output, String originalInput, RailContext context) {
        Objects.requireNonNull(output, "output must not be null");

        if (!requireValidJson) {
            return RailResult.pass(output, name());
        }

        try {
            JsonNode node = MAPPER.readTree(output.trim());
            if (node == null) {
                return RailResult.block(name(), "LLM response is not valid JSON (null node)");
            }
            return RailResult.pass(output, name());
        } catch (Exception e) {
            log.debug("JSON validation failed: {}", e.getMessage());
            return RailResult.block(name(), "LLM response is not valid JSON: " + e.getMessage());
        }
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link JsonSchemaValidator}. */
    public static final class Builder {
        private String name = "json-schema-validator";
        private boolean enabled = true;
        private int priority = 40;
        private boolean requireValidJson = true;

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder requireValidJson(boolean requireValidJson) {
            this.requireValidJson = requireValidJson;
            return this;
        }

        public JsonSchemaValidator build() {
            return new JsonSchemaValidator(this);
        }
    }
}
