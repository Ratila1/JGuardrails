package io.jguardrails.pipeline;

import io.jguardrails.audit.AuditLogger;
import io.jguardrails.audit.DefaultAuditLogger;
import io.jguardrails.core.InputRail;
import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.metrics.DefaultMetrics;
import io.jguardrails.metrics.GuardrailMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Fluent builder for constructing a {@link GuardrailPipeline}.
 *
 * <p>Example:</p>
 * <pre>{@code
 * GuardrailPipeline pipeline = GuardrailPipeline.builder()
 *     .addInputRail(new JailbreakDetector())
 *     .addInputRail(new PiiMasker(PiiEntity.EMAIL))
 *     .addOutputRail(new ToxicityChecker())
 *     .onBlocked(ctx -> "I cannot process this request.")
 *     .failOpen(false)
 *     .build();
 * }</pre>
 */
public final class PipelineBuilder {

    private final List<InputRail> inputRails = new ArrayList<>();
    private final List<OutputRail> outputRails = new ArrayList<>();
    private Function<RailContext, String> blockedResponseHandler;
    private AuditLogger auditLogger = new DefaultAuditLogger();
    private GuardrailMetrics metrics = new DefaultMetrics();
    private boolean failOpen = false;
    private String defaultBlockedResponse = "I'm unable to process this request.";

    PipelineBuilder() {}

    /**
     * Adds an input rail to the pipeline.
     *
     * @param rail input rail to add
     * @return this builder
     */
    public PipelineBuilder addInputRail(InputRail rail) {
        Objects.requireNonNull(rail, "rail must not be null");
        this.inputRails.add(rail);
        return this;
    }

    /**
     * Adds multiple input rails to the pipeline.
     *
     * @param rails input rails to add
     * @return this builder
     */
    public PipelineBuilder addInputRails(InputRail... rails) {
        Arrays.stream(rails).forEach(this::addInputRail);
        return this;
    }

    /**
     * Adds a list of input rails to the pipeline.
     *
     * @param rails input rails to add
     * @return this builder
     */
    public PipelineBuilder addInputRails(List<InputRail> rails) {
        Objects.requireNonNull(rails, "rails must not be null");
        rails.forEach(this::addInputRail);
        return this;
    }

    /**
     * Adds an output rail to the pipeline.
     *
     * @param rail output rail to add
     * @return this builder
     */
    public PipelineBuilder addOutputRail(OutputRail rail) {
        Objects.requireNonNull(rail, "rail must not be null");
        this.outputRails.add(rail);
        return this;
    }

    /**
     * Adds multiple output rails to the pipeline.
     *
     * @param rails output rails to add
     * @return this builder
     */
    public PipelineBuilder addOutputRails(OutputRail... rails) {
        Arrays.stream(rails).forEach(this::addOutputRail);
        return this;
    }

    /**
     * Adds a list of output rails to the pipeline.
     *
     * @param rails output rails to add
     * @return this builder
     */
    public PipelineBuilder addOutputRails(List<OutputRail> rails) {
        Objects.requireNonNull(rails, "rails must not be null");
        rails.forEach(this::addOutputRail);
        return this;
    }

    /**
     * Sets the function invoked to produce a response when a rail blocks a request.
     *
     * @param handler function from context to blocked-response message
     * @return this builder
     */
    public PipelineBuilder onBlocked(Function<RailContext, String> handler) {
        this.blockedResponseHandler = Objects.requireNonNull(handler, "handler must not be null");
        return this;
    }

    /**
     * Sets a static blocked-response message.
     *
     * @param message the message returned when a rail blocks the request
     * @return this builder
     */
    public PipelineBuilder blockedResponse(String message) {
        Objects.requireNonNull(message, "message must not be null");
        this.defaultBlockedResponse = message;
        this.blockedResponseHandler = ctx -> message;
        return this;
    }

    /**
     * Sets the audit logger.
     *
     * @param auditLogger audit logger implementation
     * @return this builder
     */
    public PipelineBuilder auditLogger(AuditLogger auditLogger) {
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger must not be null");
        return this;
    }

    /**
     * Sets the metrics collector.
     *
     * @param metrics metrics implementation
     * @return this builder
     */
    public PipelineBuilder metrics(GuardrailMetrics metrics) {
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        return this;
    }

    /**
     * Configures the fail strategy.
     *
     * <p>When {@code true} (fail-open), a rail that throws an exception is skipped
     * and processing continues. When {@code false} (fail-closed, default), the
     * request is blocked on any rail error.</p>
     *
     * @param failOpen whether to fail open
     * @return this builder
     */
    public PipelineBuilder failOpen(boolean failOpen) {
        this.failOpen = failOpen;
        return this;
    }

    /**
     * Builds the immutable {@link GuardrailPipeline}.
     *
     * <p>Rails are sorted by their {@link io.jguardrails.core.Rail#priority()} before execution.</p>
     *
     * @return configured pipeline
     */
    public GuardrailPipeline build() {
        List<InputRail> sortedInputRails = inputRails.stream()
                .sorted(Comparator.comparingInt(io.jguardrails.core.Rail::priority))
                .toList();
        List<OutputRail> sortedOutputRails = outputRails.stream()
                .sorted(Comparator.comparingInt(io.jguardrails.core.Rail::priority))
                .toList();

        Function<RailContext, String> handler = blockedResponseHandler != null
                ? blockedResponseHandler
                : ctx -> defaultBlockedResponse;

        PipelineConfig config = new PipelineConfig(
                sortedInputRails,
                sortedOutputRails,
                handler,
                auditLogger,
                metrics,
                failOpen,
                defaultBlockedResponse
        );

        return new GuardrailPipeline(config);
    }
}
