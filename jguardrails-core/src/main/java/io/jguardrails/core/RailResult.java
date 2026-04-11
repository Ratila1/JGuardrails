package io.jguardrails.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The immutable result of a single rail's processing step.
 *
 * <p>Use the static factory methods to create instances:</p>
 * <ul>
 *   <li>{@link #pass(String, String)} – allowed unchanged</li>
 *   <li>{@link #block(String, String)} – blocked with a reason</li>
 *   <li>{@link #modify(String, String, String)} – modified with explanation</li>
 * </ul>
 *
 * @param action     the rail decision: PASS, BLOCK, or MODIFY
 * @param text       the text (original for PASS, transformed for MODIFY, {@code null} for BLOCK)
 * @param railName   the name of the rail that produced this result
 * @param reason     the reason for blocking or modifying (may be {@code null} for PASS)
 * @param confidence detector confidence score in range [0.0, 1.0], or -1 if not applicable
 * @param metadata   additional data about the decision (e.g., detected PII types, matched pattern)
 */
public record RailResult(
        RailAction action,
        String text,
        String railName,
        String reason,
        double confidence,
        Map<String, Object> metadata
) {

    /**
     * Compact constructor enforcing non-null invariants.
     */
    public RailResult {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(railName, "railName must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a PASS result — the text is allowed through unchanged.
     *
     * @param text     the original text
     * @param railName the name of the rail
     * @return a PASS result
     */
    public static RailResult pass(String text, String railName) {
        return new RailResult(RailAction.PASS, text, railName, null, -1, Map.of());
    }

    /**
     * Creates a BLOCK result — the text is rejected with a reason.
     *
     * @param railName the name of the rail
     * @param reason   human-readable reason for the block
     * @return a BLOCK result
     */
    public static RailResult block(String railName, String reason) {
        return new RailResult(RailAction.BLOCK, null, railName, reason, -1, Map.of());
    }

    /**
     * Creates a BLOCK result with a confidence score.
     *
     * @param railName   the name of the rail
     * @param reason     human-readable reason for the block
     * @param confidence detector confidence [0.0, 1.0]
     * @return a BLOCK result
     */
    public static RailResult block(String railName, String reason, double confidence) {
        return new RailResult(RailAction.BLOCK, null, railName, reason, confidence, Map.of());
    }

    /**
     * Creates a BLOCK result with additional metadata.
     *
     * @param railName   the name of the rail
     * @param reason     human-readable reason for the block
     * @param confidence detector confidence [0.0, 1.0]
     * @param metadata   additional information about the detection
     * @return a BLOCK result
     */
    public static RailResult block(String railName, String reason, double confidence, Map<String, Object> metadata) {
        return new RailResult(RailAction.BLOCK, null, railName, reason, confidence, metadata);
    }

    /**
     * Creates a MODIFY result — the text is replaced with a sanitised version.
     *
     * @param newText  the modified text
     * @param railName the name of the rail
     * @param reason   explanation of what was changed
     * @return a MODIFY result
     */
    public static RailResult modify(String newText, String railName, String reason) {
        return new RailResult(RailAction.MODIFY, newText, railName, reason, -1, Map.of());
    }

    /**
     * Creates a MODIFY result with additional metadata.
     *
     * @param newText  the modified text
     * @param railName the name of the rail
     * @param reason   explanation of what was changed
     * @param metadata additional information (e.g., list of masked PII entities)
     * @return a MODIFY result
     */
    public static RailResult modify(String newText, String railName, String reason, Map<String, Object> metadata) {
        return new RailResult(RailAction.MODIFY, newText, railName, reason, -1, metadata);
    }

    // -------------------------------------------------------------------------
    // Convenience accessors
    // -------------------------------------------------------------------------

    /** @return {@code true} if the action is BLOCK */
    public boolean isBlocked() {
        return action == RailAction.BLOCK;
    }

    /** @return {@code true} if the action is MODIFY */
    public boolean isModified() {
        return action == RailAction.MODIFY;
    }

    /** @return {@code true} if the action is PASS */
    public boolean isPassed() {
        return action == RailAction.PASS;
    }

    /**
     * Returns the reason wrapped in an {@link Optional}.
     *
     * @return optional reason
     */
    public Optional<String> optionalReason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Returns the text wrapped in an {@link Optional}.
     *
     * @return optional text (empty for BLOCK results)
     */
    public Optional<String> optionalText() {
        return Optional.ofNullable(text);
    }
}
