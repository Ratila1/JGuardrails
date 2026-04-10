package io.jguardrails.spring;

import io.jguardrails.core.RailContext;
import io.jguardrails.pipeline.GuardrailPipeline;
import io.jguardrails.pipeline.PipelineExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Objects;

/**
 * Spring AI {@link CallAroundAdvisor} that applies guardrail rails to all ChatClient calls.
 *
 * <p>Integrate via ChatClient builder:</p>
 * <pre>{@code
 * @Bean
 * public ChatClient chatClient(ChatClient.Builder builder, GuardrailPipeline pipeline) {
 *     return builder
 *         .defaultAdvisors(new GuardrailAdvisor(pipeline))
 *         .build();
 * }
 * }</pre>
 *
 * <p>Or rely on Spring Boot auto-configuration by adding {@code jguardrails-spring-ai}
 * to the classpath and configuring {@code application.yml}.</p>
 */
public class GuardrailAdvisor implements CallAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(GuardrailAdvisor.class);
    private static final int DEFAULT_ORDER = 0;

    private final GuardrailPipeline pipeline;
    private final int order;

    /**
     * Creates a guardrail advisor with the given pipeline.
     *
     * @param pipeline the configured guardrail pipeline
     */
    public GuardrailAdvisor(GuardrailPipeline pipeline) {
        this(pipeline, DEFAULT_ORDER);
    }

    /**
     * Creates a guardrail advisor with explicit ordering.
     *
     * @param pipeline the configured guardrail pipeline
     * @param order    Spring advisor chain ordering (lower = earlier)
     */
    public GuardrailAdvisor(GuardrailPipeline pipeline, int order) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
        this.order = order;
    }

    @Override
    public String getName() {
        return "JGuardrailsAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        String userText = advisedRequest.userText();
        RailContext context = buildContext(advisedRequest);

        // Process input through guardrails
        PipelineExecutionResult inputResult = pipeline.processInput(userText, context);
        if (inputResult.isBlocked()) {
            log.info("GuardrailAdvisor: input blocked by pipeline");
            return AdvisedResponse.from(advisedRequest)
                    .withResponse(buildBlockedChatResponse(inputResult.getText()))
                    .build();
        }

        // Proceed with (possibly modified) input
        AdvisedRequest modifiedRequest = advisedRequest.toBuilder()
                .userText(inputResult.getText())
                .build();

        AdvisedResponse response = chain.nextAroundCall(modifiedRequest);

        // Process output through guardrails
        String llmOutput = extractResponseText(response);
        PipelineExecutionResult outputResult = pipeline.processOutput(llmOutput, userText, context);

        if (outputResult.isBlocked()) {
            log.info("GuardrailAdvisor: output blocked by pipeline");
            return AdvisedResponse.from(advisedRequest)
                    .withResponse(buildBlockedChatResponse(outputResult.getText()))
                    .build();
        }

        return response;
    }

    private RailContext buildContext(AdvisedRequest request) {
        RailContext.Builder builder = RailContext.builder();
        if (request.chatModel() != null) {
            builder.attribute("spring.ai.model", request.chatModel().getClass().getSimpleName());
        }
        return builder.build();
    }

    private String extractResponseText(AdvisedResponse response) {
        try {
            return response.response().getResult().getOutput().getContent();
        } catch (Exception e) {
            return "";
        }
    }

    private ChatResponse buildBlockedChatResponse(String message) {
        Generation generation = new Generation(new AssistantMessage(message));
        return new ChatResponse(List.of(generation));
    }
}
