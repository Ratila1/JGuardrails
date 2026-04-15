package io.jguardrails.pipeline;

import io.jguardrails.audit.AuditEntry;
import io.jguardrails.core.BlockedRequestException;
import io.jguardrails.core.InputRail;
import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import io.jguardrails.normalize.TextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The central guardrail pipeline.
 *
 * <p>Processes text through a chain of input and output rails:</p>
 * <ol>
 *   <li>User input is run through all {@link InputRail}s in priority order.</li>
 *   <li>If any rail returns BLOCK, processing stops and a blocked response is returned.</li>
 *   <li>The (possibly modified) input is returned to the caller for LLM invocation.</li>
 *   <li>The LLM response is run through all {@link OutputRail}s in priority order.</li>
 *   <li>The final (possibly modified) response is returned to the user.</li>
 * </ol>
 *
 * <p><strong>Important:</strong> the pipeline does <em>not</em> call the LLM itself.
 * That is the responsibility of the calling code or an adapter (Spring AI, LangChain4j).</p>
 *
 * <p>Create instances via the fluent builder:</p>
 * <pre>{@code
 * GuardrailPipeline pipeline = GuardrailPipeline.builder()
 *     .addInputRail(new JailbreakDetector())
 *     .addInputRail(new PiiMasker(PiiEntity.EMAIL, PiiEntity.PHONE))
 *     .addOutputRail(new ToxicityChecker())
 *     .onBlocked(ctx -> "Sorry, I cannot process this request.")
 *     .build();
 *
 * // Step 1: process input
 * PipelineExecutionResult inputResult = pipeline.processInput(userMessage, context);
 * if (inputResult.isBlocked()) {
 *     return inputResult.getText(); // blocked-response message
 * }
 *
 * // Step 2: call LLM (outside pipeline)
 * String llmResponse = myLlmClient.chat(inputResult.getText());
 *
 * // Step 3: process output
 * PipelineExecutionResult outputResult = pipeline.processOutput(llmResponse, userMessage, context);
 * return outputResult.getText();
 * }</pre>
 *
 * <p>Or use the single-call helper with a callback:</p>
 * <pre>{@code
 * String safeResponse = pipeline.execute(userMessage, context,
 *     processedInput -> myLlmClient.chat(processedInput));
 * }</pre>
 *
 * <p>This class is immutable and thread-safe.</p>
 */
public final class GuardrailPipeline {

    private static final Logger log = LoggerFactory.getLogger(GuardrailPipeline.class);

    private final PipelineConfig config;

    GuardrailPipeline(PipelineConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Creates a new pipeline builder.
     *
     * @return new builder
     */
    public static PipelineBuilder builder() {
        return new PipelineBuilder();
    }

    // -------------------------------------------------------------------------
    // Pipeline execution
    // -------------------------------------------------------------------------

    /**
     * Processes user input through all configured input rails.
     *
     * @param input   user message text
     * @param context execution context
     * @return execution result (check {@link PipelineExecutionResult#isBlocked()} before calling LLM)
     */
    public PipelineExecutionResult processInput(String input, RailContext context) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Instant start = Instant.now();
        List<RailResult> railResults = new ArrayList<>();
        String currentText = input;

        // Normalize once and store in context so detectors don't repeat the work.
        // PII masking rails always receive the un-normalized currentText (original).
        String normalizedInput = config.getNormalizer().normalize(input);
        context.setAttribute(TextNormalizer.CONTEXT_KEY, normalizedInput);
        log.debug("Input normalized for detection: '{}'", normalizedInput);

        for (InputRail rail : config.getInputRails()) {
            if (!rail.isEnabled()) {
                continue;
            }
            try {
                RailResult result = rail.process(currentText, context);
                railResults.add(result);
                logRailResult("INPUT", rail.name(), result);

                if (result.isBlocked()) {
                    config.getMetrics().recordBlock(rail.name());
                    config.getAuditLogger().log(AuditEntry.blocked(input, rail.name(), result.reason()));
                    String blockedResponse = config.getBlockedResponseHandler().apply(context);
                    return PipelineExecutionResult.builder()
                            .text(blockedResponse)
                            .originalText(input)
                            .blocked(true)
                            .blockingResult(result)
                            .railResults(railResults)
                            .executionTime(Duration.between(start, Instant.now()))
                            .build();
                }

                if (result.isModified()) {
                    config.getMetrics().recordModification(rail.name());
                    config.getAuditLogger().log(AuditEntry.modified(input, result.text(), rail.name(), result.reason()));
                    currentText = result.text();
                }

            } catch (Exception e) {
                log.error("Rail '{}' threw an exception during input processing", rail.name(), e);
                if (!config.isFailOpen()) {
                    config.getMetrics().recordError(rail.name());
                    String errorResponse = config.getBlockedResponseHandler().apply(context);
                    return PipelineExecutionResult.builder()
                            .text(errorResponse)
                            .originalText(input)
                            .blocked(true)
                            .railResults(railResults)
                            .executionTime(Duration.between(start, Instant.now()))
                            .build();
                }
                log.warn("Fail-open: skipping rail '{}' after error", rail.name());
            }
        }

        config.getMetrics().recordPass("input");
        return PipelineExecutionResult.builder()
                .text(currentText)
                .originalText(input)
                .blocked(false)
                .railResults(railResults)
                .executionTime(Duration.between(start, Instant.now()))
                .build();
    }

    /**
     * Processes LLM output through all configured output rails.
     *
     * @param output        LLM response text
     * @param originalInput the original user request (for relevance checks)
     * @param context       execution context
     * @return execution result with processed text
     */
    public PipelineExecutionResult processOutput(String output, String originalInput, RailContext context) {
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(originalInput, "originalInput must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Instant start = Instant.now();
        List<RailResult> railResults = new ArrayList<>();
        String currentText = output;

        for (OutputRail rail : config.getOutputRails()) {
            if (!rail.isEnabled()) {
                continue;
            }
            try {
                RailResult result = rail.process(currentText, originalInput, context);
                railResults.add(result);
                logRailResult("OUTPUT", rail.name(), result);

                if (result.isBlocked()) {
                    config.getMetrics().recordBlock(rail.name());
                    config.getAuditLogger().log(AuditEntry.blocked(output, rail.name(), result.reason()));
                    String blockedResponse = config.getBlockedResponseHandler().apply(context);
                    return PipelineExecutionResult.builder()
                            .text(blockedResponse)
                            .originalText(output)
                            .blocked(true)
                            .blockingResult(result)
                            .railResults(railResults)
                            .executionTime(Duration.between(start, Instant.now()))
                            .build();
                }

                if (result.isModified()) {
                    config.getMetrics().recordModification(rail.name());
                    config.getAuditLogger().log(AuditEntry.modified(output, result.text(), rail.name(), result.reason()));
                    currentText = result.text();
                }

            } catch (Exception e) {
                log.error("Rail '{}' threw an exception during output processing", rail.name(), e);
                if (!config.isFailOpen()) {
                    config.getMetrics().recordError(rail.name());
                    String errorResponse = config.getBlockedResponseHandler().apply(context);
                    return PipelineExecutionResult.builder()
                            .text(errorResponse)
                            .originalText(output)
                            .blocked(true)
                            .railResults(railResults)
                            .executionTime(Duration.between(start, Instant.now()))
                            .build();
                }
                log.warn("Fail-open: skipping rail '{}' after error", rail.name());
            }
        }

        config.getMetrics().recordPass("output");
        return PipelineExecutionResult.builder()
                .text(currentText)
                .originalText(output)
                .blocked(false)
                .railResults(railResults)
                .executionTime(Duration.between(start, Instant.now()))
                .build();
    }

    /**
     * Executes a full guardrail-guarded LLM interaction in a single call.
     *
     * <p>The {@code llmInvoker} function is called only if input rails pass.
     * Its output is then passed through output rails.</p>
     *
     * @param userInput   the raw user message
     * @param context     execution context
     * @param llmInvoker  function that calls the LLM and returns its response
     * @return the final safe response (or blocked-response message)
     * @throws BlockedRequestException if throwOnBlock is configured (not default)
     */
    public String execute(String userInput, RailContext context, Function<String, String> llmInvoker) {
        Objects.requireNonNull(userInput, "userInput must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(llmInvoker, "llmInvoker must not be null");

        PipelineExecutionResult inputResult = processInput(userInput, context);
        if (inputResult.isBlocked()) {
            return inputResult.getText();
        }

        String llmResponse = llmInvoker.apply(inputResult.getText());

        PipelineExecutionResult outputResult = processOutput(llmResponse, userInput, context);
        return outputResult.getText();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void logRailResult(String phase, String railName, RailResult result) {
        if (result.isBlocked()) {
            log.info("[{}][{}] BLOCK — reason: {}", phase, railName, result.reason());
        } else if (result.isModified()) {
            log.debug("[{}][{}] MODIFY — reason: {}", phase, railName, result.reason());
        } else {
            log.debug("[{}][{}] PASS", phase, railName);
        }
    }
}
