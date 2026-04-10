package io.jguardrails.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.jguardrails.core.RailContext;
import io.jguardrails.pipeline.GuardrailPipeline;
import io.jguardrails.pipeline.PipelineExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A LangChain4j {@link ChatLanguageModel} wrapper that applies guardrail rails
 * to all {@code generate()} calls transparently.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ChatLanguageModel baseModel = OpenAiChatModel.builder()
 *     .apiKey("...")
 *     .modelName("gpt-4o")
 *     .build();
 *
 * GuardrailPipeline pipeline = GuardrailPipeline.builder()
 *     .addInputRail(new JailbreakDetector())
 *     .addOutputRail(new ToxicityChecker())
 *     .build();
 *
 * ChatLanguageModel guardedModel = new GuardrailChatModelFilter(baseModel, pipeline);
 *
 * // All calls go through guardrails automatically
 * String response = guardedModel.generate("Tell me about Java");
 * }</pre>
 */
public class GuardrailChatModelFilter implements ChatLanguageModel {

    private static final Logger log = LoggerFactory.getLogger(GuardrailChatModelFilter.class);

    private final ChatLanguageModel delegate;
    private final GuardrailPipeline pipeline;

    /**
     * Wraps a base {@link ChatLanguageModel} with guardrail protection.
     *
     * @param delegate the underlying LLM to delegate to
     * @param pipeline the guardrail pipeline to apply
     */
    public GuardrailChatModelFilter(ChatLanguageModel delegate, GuardrailPipeline pipeline) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        String userInput = extractUserInput(messages);
        RailContext context = RailContext.builder().build();

        // Apply input rails
        PipelineExecutionResult inputResult = pipeline.processInput(userInput, context);
        if (inputResult.isBlocked()) {
            log.info("GuardrailChatModelFilter: input blocked — returning safe response");
            return Response.from(AiMessage.from(inputResult.getText()));
        }

        // Delegate to underlying model with (possibly modified) input
        List<ChatMessage> processedMessages = replaceLastUserMessage(messages, inputResult.getText());
        Response<AiMessage> llmResponse = delegate.generate(processedMessages);

        // Apply output rails
        String llmOutput = llmResponse.content().text();
        PipelineExecutionResult outputResult = pipeline.processOutput(llmOutput, userInput, context);

        if (outputResult.isBlocked()) {
            log.info("GuardrailChatModelFilter: output blocked — returning safe response");
            return Response.from(AiMessage.from(outputResult.getText()),
                    llmResponse.tokenUsage(), llmResponse.finishReason());
        }

        if (!llmOutput.equals(outputResult.getText())) {
            return Response.from(AiMessage.from(outputResult.getText()),
                    llmResponse.tokenUsage(), llmResponse.finishReason());
        }

        return llmResponse;
    }

    private String extractUserInput(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message instanceof UserMessage userMessage) {
                return userMessage.singleText();
            }
        }
        return "";
    }

    private List<ChatMessage> replaceLastUserMessage(List<ChatMessage> messages, String newText) {
        if (messages.isEmpty()) return messages;
        ArrayList<ChatMessage> result = new ArrayList<>(messages);
        for (int i = result.size() - 1; i >= 0; i--) {
            if (result.get(i) instanceof UserMessage) {
                result.set(i, UserMessage.from(newText));
                break;
            }
        }
        return List.copyOf(result);
    }
}
