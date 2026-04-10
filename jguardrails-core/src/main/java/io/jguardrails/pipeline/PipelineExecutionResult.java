package io.jguardrails.pipeline;

import io.jguardrails.core.RailResult;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The complete result of a pipeline execution phase (input or output processing).
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>The final processed text (or blocked-response message)</li>
 *   <li>Whether the request was blocked and by which rail</li>
 *   <li>All individual rail results for auditing and debugging</li>
 *   <li>Total pipeline execution duration</li>
 *   <li>The original text before any rails were applied</li>
 * </ul>
 */
public final class PipelineExecutionResult {

    private final String text;
    private final String originalText;
    private final boolean blocked;
    private final RailResult blockingResult;
    private final List<RailResult> railResults;
    private final Duration executionTime;

    private PipelineExecutionResult(Builder builder) {
        this.text = builder.text;
        this.originalText = builder.originalText;
        this.blocked = builder.blocked;
        this.blockingResult = builder.blockingResult;
        this.railResults = List.copyOf(builder.railResults);
        this.executionTime = builder.executionTime;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the final text after pipeline processing.
     * For blocked results, this is the blocked-response fallback message.
     *
     * @return final text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the original text before any rails were applied.
     *
     * @return original text
     */
    public String getOriginalText() {
        return originalText;
    }

    /**
     * Returns {@code true} if the pipeline blocked this request.
     *
     * @return whether the request was blocked
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Returns the rail result that caused the block, if any.
     *
     * @return optional blocking rail result
     */
    public Optional<RailResult> getBlockingResult() {
        return Optional.ofNullable(blockingResult);
    }

    /**
     * Returns an unmodifiable list of all rail results in execution order.
     *
     * @return list of rail results
     */
    public List<RailResult> getRailResults() {
        return railResults;
    }

    /**
     * Returns the total time spent executing all rails.
     *
     * @return pipeline execution duration
     */
    public Duration getExecutionTime() {
        return executionTime;
    }

    /**
     * Returns the blocked-response message if the pipeline was blocked.
     *
     * @return optional blocked-response text
     */
    public Optional<String> getBlockedResponse() {
        if (blocked) {
            return Optional.ofNullable(text);
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link PipelineExecutionResult}. */
    public static final class Builder {

        private String text;
        private String originalText;
        private boolean blocked;
        private RailResult blockingResult;
        private List<RailResult> railResults = List.of();
        private Duration executionTime = Duration.ZERO;

        private Builder() {}

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder originalText(String originalText) {
            this.originalText = originalText;
            return this;
        }

        public Builder blocked(boolean blocked) {
            this.blocked = blocked;
            return this;
        }

        public Builder blockingResult(RailResult blockingResult) {
            this.blockingResult = blockingResult;
            return this;
        }

        public Builder railResults(List<RailResult> railResults) {
            this.railResults = Objects.requireNonNull(railResults);
            return this;
        }

        public Builder executionTime(Duration executionTime) {
            this.executionTime = Objects.requireNonNull(executionTime);
            return this;
        }

        public PipelineExecutionResult build() {
            return new PipelineExecutionResult(this);
        }
    }
}
