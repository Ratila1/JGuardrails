package io.jguardrails.detectors.output.length;

import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;

import java.util.Objects;

/**
 * Validates or truncates LLM responses that exceed configured length limits.
 *
 * <p>When {@link Builder#truncate(boolean) truncate} is {@code true}, the response
 * is shortened rather than blocked.</p>
 */
public class OutputLengthValidator implements OutputRail {

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final int maxCharacters;
    private final boolean truncate;

    private OutputLengthValidator(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.maxCharacters = builder.maxCharacters;
        this.truncate = builder.truncate;
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

        if (maxCharacters > 0 && output.length() > maxCharacters) {
            if (truncate) {
                String truncated = output.substring(0, maxCharacters) + "...";
                return RailResult.modify(truncated, name(),
                        "Response truncated to " + maxCharacters + " characters");
            }
            return RailResult.block(name(),
                    String.format("Response exceeds maximum length: %d characters (limit: %d)",
                            output.length(), maxCharacters));
        }

        return RailResult.pass(output, name());
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link OutputLengthValidator}. */
    public static final class Builder {
        private String name = "output-length-validator";
        private boolean enabled = true;
        private int priority = 30;
        private int maxCharacters = 5_000;
        private boolean truncate = false;

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder maxCharacters(int maxCharacters) { this.maxCharacters = maxCharacters; return this; }

        /** If {@code true}, truncate the response instead of blocking it. */
        public Builder truncate(boolean truncate) { this.truncate = truncate; return this; }

        public OutputLengthValidator build() {
            return new OutputLengthValidator(this);
        }
    }
}
