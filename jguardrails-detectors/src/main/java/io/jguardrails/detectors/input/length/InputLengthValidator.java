package io.jguardrails.detectors.input.length;

import io.jguardrails.core.InputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;

import java.util.Objects;

/**
 * Validates that user input does not exceed configured length limits.
 *
 * <p>Prevents excessively long inputs that could cause context-overflow attacks
 * or unexpectedly high LLM costs.</p>
 *
 * <p>Can limit by character count and/or word count.</p>
 */
public class InputLengthValidator implements InputRail {

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final int maxCharacters;
    private final int maxWords;

    private InputLengthValidator(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.maxCharacters = builder.maxCharacters;
        this.maxWords = builder.maxWords;
    }

    @Override
    public String name() { return name; }

    @Override
    public int priority() { return priority; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public RailResult process(String input, RailContext context) {
        Objects.requireNonNull(input, "input must not be null");

        if (maxCharacters > 0 && input.length() > maxCharacters) {
            return RailResult.block(name(),
                    String.format("Input exceeds maximum length: %d characters (limit: %d)",
                            input.length(), maxCharacters));
        }

        if (maxWords > 0) {
            long wordCount = input.trim().isEmpty() ? 0
                    : input.trim().split("\\s+").length;
            if (wordCount > maxWords) {
                return RailResult.block(name(),
                        String.format("Input exceeds maximum word count: %d words (limit: %d)",
                                wordCount, maxWords));
            }
        }

        return RailResult.pass(input, name());
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link InputLengthValidator}. */
    public static final class Builder {
        private String name = "input-length-validator";
        private boolean enabled = true;
        private int priority = 5;
        private int maxCharacters = 10_000;
        private int maxWords = 0;

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }

        /** Sets the maximum allowed character count (0 = disabled). */
        public Builder maxCharacters(int maxCharacters) { this.maxCharacters = maxCharacters; return this; }

        /** Sets the maximum allowed word count (0 = disabled). */
        public Builder maxWords(int maxWords) { this.maxWords = maxWords; return this; }

        public InputLengthValidator build() {
            return new InputLengthValidator(this);
        }
    }
}
