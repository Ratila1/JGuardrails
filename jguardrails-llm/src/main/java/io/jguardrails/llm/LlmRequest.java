package io.jguardrails.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A request to a language model.
 *
 * <p>Contains a list of messages in OpenAI-compatible format
 * (system, user, assistant roles).</p>
 */
public final class LlmRequest {

    /** A single message in the conversation. */
    public record Message(String role, String content) {

        public Message {
            Objects.requireNonNull(role, "role must not be null");
            Objects.requireNonNull(content, "content must not be null");
        }

        /** Creates a system message. */
        public static Message system(String content) { return new Message("system", content); }

        /** Creates a user message. */
        public static Message user(String content) { return new Message("user", content); }

        /** Creates an assistant message. */
        public static Message assistant(String content) { return new Message("assistant", content); }
    }

    private final String model;
    private final List<Message> messages;
    private final double temperature;
    private final int maxTokens;

    private LlmRequest(Builder builder) {
        this.model = Objects.requireNonNull(builder.model, "model must not be null");
        this.messages = List.copyOf(builder.messages);
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
    }

    /** @return the model name */
    public String getModel() { return model; }

    /** @return the list of messages */
    public List<Message> getMessages() { return messages; }

    /** @return the temperature setting */
    public double getTemperature() { return temperature; }

    /** @return the maximum tokens to generate */
    public int getMaxTokens() { return maxTokens; }

    /**
     * Creates a new builder for an {@link LlmRequest}.
     *
     * @param model the model name
     * @return new builder
     */
    public static Builder builder(String model) {
        return new Builder(model);
    }

    /** Builder for {@link LlmRequest}. */
    public static final class Builder {
        private final String model;
        private final ArrayList<Message> messages = new ArrayList<>();
        private double temperature = 0.0;
        private int maxTokens = 256;

        private Builder(String model) {
            this.model = model;
        }

        public Builder addMessage(Message message) { messages.add(message); return this; }
        public Builder systemMessage(String content) { return addMessage(Message.system(content)); }
        public Builder userMessage(String content) { return addMessage(Message.user(content)); }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }

        public LlmRequest build() { return new LlmRequest(this); }
    }
}
