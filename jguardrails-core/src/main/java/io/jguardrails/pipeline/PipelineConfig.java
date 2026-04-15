package io.jguardrails.pipeline;

import io.jguardrails.audit.AuditLogger;
import io.jguardrails.core.InputRail;
import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.metrics.GuardrailMetrics;
import io.jguardrails.normalize.TextNormalizer;

import java.util.List;
import java.util.function.Function;

/**
 * Immutable configuration snapshot for a {@link GuardrailPipeline}.
 *
 * <p>Built via {@link PipelineBuilder} and used internally by the pipeline.</p>
 */
public final class PipelineConfig {

    private final List<InputRail> inputRails;
    private final List<OutputRail> outputRails;
    private final Function<RailContext, String> blockedResponseHandler;
    private final AuditLogger auditLogger;
    private final GuardrailMetrics metrics;
    private final boolean failOpen;
    private final String defaultBlockedResponse;
    private final TextNormalizer normalizer;

    PipelineConfig(
            List<InputRail> inputRails,
            List<OutputRail> outputRails,
            Function<RailContext, String> blockedResponseHandler,
            AuditLogger auditLogger,
            GuardrailMetrics metrics,
            boolean failOpen,
            String defaultBlockedResponse,
            TextNormalizer normalizer) {
        this.inputRails = List.copyOf(inputRails);
        this.outputRails = List.copyOf(outputRails);
        this.blockedResponseHandler = blockedResponseHandler;
        this.auditLogger = auditLogger;
        this.metrics = metrics;
        this.failOpen = failOpen;
        this.defaultBlockedResponse = defaultBlockedResponse;
        this.normalizer = normalizer;
    }

    /** @return ordered list of input rails */
    public List<InputRail> getInputRails() {
        return inputRails;
    }

    /** @return ordered list of output rails */
    public List<OutputRail> getOutputRails() {
        return outputRails;
    }

    /** @return handler that produces the blocked-response message */
    public Function<RailContext, String> getBlockedResponseHandler() {
        return blockedResponseHandler;
    }

    /** @return audit logger */
    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    /** @return metrics collector */
    public GuardrailMetrics getMetrics() {
        return metrics;
    }

    /**
     * Returns whether to fail open (allow) or fail closed (block) when a rail throws an exception.
     *
     * @return {@code true} to pass on rail error, {@code false} to block
     */
    public boolean isFailOpen() {
        return failOpen;
    }

    /** @return the default blocked-response message */
    public String getDefaultBlockedResponse() {
        return defaultBlockedResponse;
    }

    /**
     * Returns the text normalizer applied once before all input rails.
     *
     * @return text normalizer
     */
    public TextNormalizer getNormalizer() {
        return normalizer;
    }
}
