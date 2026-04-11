package io.jguardrails.llm;

import java.util.Objects;

/**
 * A response from a language model.
 */
public final class LlmResponse {

    private final String content;
    private final String model;
    private final int promptTokens;
    private final int completionTokens;

    /**
     * Constructs a full LLM response.
     *
     * @param content          the generated text content
     * @param model            the model that produced the response
     * @param promptTokens     number of prompt tokens consumed
     * @param completionTokens number of completion tokens generated
     */
    public LlmResponse(String content, String model, int promptTokens, int completionTokens) {
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    /**
     * Convenience factory for simple text responses.
     *
     * @param content the response text
     * @return a minimal response object
     */
    public static LlmResponse of(String content) {
        return new LlmResponse(content, "unknown", 0, 0);
    }

    /** @return the text content of the response */
    public String getContent() { return content; }

    /** @return the model that generated the response */
    public String getModel() { return model; }

    /** @return number of prompt tokens used */
    public int getPromptTokens() { return promptTokens; }

    /** @return number of completion tokens generated */
    public int getCompletionTokens() { return completionTokens; }

    /** @return total tokens (prompt + completion) */
    public int getTotalTokens() { return promptTokens + completionTokens; }
}
