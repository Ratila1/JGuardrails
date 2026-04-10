package io.jguardrails.langchain4j;

import io.jguardrails.core.RailContext;
import io.jguardrails.pipeline.GuardrailPipeline;
import io.jguardrails.pipeline.PipelineExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;

/**
 * Helper for manually wrapping LangChain4j AiServices calls with guardrail protection.
 *
 * <p>LangChain4j AiServices do not natively support interceptors, so this helper
 * wraps the invoke pattern manually.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * GuardrailAiServiceInterceptor interceptor = new GuardrailAiServiceInterceptor(pipeline);
 *
 * // Instead of: String response = aiService.chat(userInput);
 * // Use:
 * String response = interceptor.intercept(userInput, processedInput -> aiService.chat(processedInput));
 * }</pre>
 */
public class GuardrailAiServiceInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GuardrailAiServiceInterceptor.class);

    private final GuardrailPipeline pipeline;

    /**
     * Creates an interceptor backed by the given pipeline.
     *
     * @param pipeline the guardrail pipeline
     */
    public GuardrailAiServiceInterceptor(GuardrailPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
    }

    /**
     * Intercepts a call to an AiService method, applying input and output guardrails.
     *
     * @param userInput the raw user input
     * @param aiInvoker function that calls the AI service and returns its response
     * @return the guardrail-processed response
     */
    public String intercept(String userInput, Function<String, String> aiInvoker) {
        Objects.requireNonNull(userInput, "userInput must not be null");
        Objects.requireNonNull(aiInvoker, "aiInvoker must not be null");

        RailContext context = RailContext.builder().build();

        PipelineExecutionResult inputResult = pipeline.processInput(userInput, context);
        if (inputResult.isBlocked()) {
            log.info("GuardrailAiServiceInterceptor: input blocked");
            return inputResult.getText();
        }

        String aiResponse = aiInvoker.apply(inputResult.getText());

        PipelineExecutionResult outputResult = pipeline.processOutput(aiResponse, userInput, context);
        return outputResult.getText();
    }
}
