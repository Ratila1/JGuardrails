package io.jguardrails.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context passed through the entire guardrail pipeline.
 *
 * <p>Provides shared state across rails: conversation history, session/user identifiers,
 * and an arbitrary attribute map that rails can read from and write to.</p>
 *
 * <p>Thread-safe: the attribute map uses {@link ConcurrentHashMap}.</p>
 *
 * <p>Create instances via {@link Builder}:</p>
 * <pre>{@code
 * RailContext ctx = RailContext.builder()
 *     .sessionId("session-123")
 *     .userId("user-456")
 *     .addHistory("previous message")
 *     .attribute("language", "en")
 *     .build();
 * }</pre>
 */
public final class RailContext {

    private final Map<String, Object> attributes;
    private final List<String> conversationHistory;
    private final String sessionId;
    private final String userId;

    private RailContext(Builder builder) {
        this.attributes = new ConcurrentHashMap<>(builder.attributes);
        this.conversationHistory = Collections.unmodifiableList(new ArrayList<>(builder.conversationHistory));
        this.sessionId = builder.sessionId;
        this.userId = builder.userId;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /** Creates an empty context with no session or user info. */
    public static RailContext empty() {
        return builder().build();
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Attribute access
    // -------------------------------------------------------------------------

    /**
     * Returns the value of a context attribute.
     *
     * @param key attribute key
     * @return optional value
     */
    public Optional<Object> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    /**
     * Returns a typed attribute value.
     *
     * @param key  attribute key
     * @param type expected type class
     * @param <T>  expected type
     * @return optional typed value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Sets a context attribute. Can be used by rails to share state downstream.
     *
     * @param key   attribute key
     * @param value attribute value (must not be null)
     */
    public void setAttribute(String key, Object value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        attributes.put(key, value);
    }

    /**
     * Returns an immutable snapshot of all attributes.
     *
     * @return attribute map snapshot
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    // -------------------------------------------------------------------------
    // Conversation history
    // -------------------------------------------------------------------------

    /**
     * Returns the conversation history (previous messages in the dialog).
     *
     * @return unmodifiable list of previous messages
     */
    public List<String> getConversationHistory() {
        return conversationHistory;
    }

    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    /**
     * Returns the session identifier, if present.
     *
     * @return optional session ID
     */
    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId);
    }

    /**
     * Returns the user identifier, if present.
     *
     * @return optional user ID
     */
    public Optional<String> getUserId() {
        return Optional.ofNullable(userId);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Builder for {@link RailContext}. */
    public static final class Builder {

        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private final List<String> conversationHistory = new ArrayList<>();
        private String sessionId;
        private String userId;

        private Builder() {}

        /**
         * Sets the session identifier.
         *
         * @param sessionId session ID
         * @return this builder
         */
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Sets the user identifier.
         *
         * @param userId user ID
         * @return this builder
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Adds a message to the conversation history.
         *
         * @param message previous message text
         * @return this builder
         */
        public Builder addHistory(String message) {
            this.conversationHistory.add(message);
            return this;
        }

        /**
         * Sets the full conversation history.
         *
         * @param history list of previous messages
         * @return this builder
         */
        public Builder history(List<String> history) {
            this.conversationHistory.clear();
            this.conversationHistory.addAll(history);
            return this;
        }

        /**
         * Adds a context attribute.
         *
         * @param key   attribute key
         * @param value attribute value
         * @return this builder
         */
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        /**
         * Builds the {@link RailContext}.
         *
         * @return immutable context
         */
        public RailContext build() {
            return new RailContext(this);
        }
    }
}
