package io.jguardrails.audit;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable audit record produced when a rail blocks or modifies text.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>Timestamp of the event</li>
 *   <li>Original text (before rail processing)</li>
 *   <li>Result text (after modification; {@code null} if blocked)</li>
 *   <li>The rail that produced the event</li>
 *   <li>The reason for the action</li>
 *   <li>The type of event: BLOCKED or MODIFIED</li>
 * </ul>
 */
public final class AuditEntry {

    /** Type of guardrail event. */
    public enum Type {
        /** The request or response was blocked. */
        BLOCKED,
        /** The text was modified (e.g., PII masked). */
        MODIFIED
    }

    private final Instant timestamp;
    private final String originalText;
    private final String resultText;
    private final String railName;
    private final String reason;
    private final Type type;

    private AuditEntry(Instant timestamp, String originalText, String resultText,
                       String railName, String reason, Type type) {
        this.timestamp = Objects.requireNonNull(timestamp);
        this.originalText = originalText;
        this.resultText = resultText;
        this.railName = Objects.requireNonNull(railName);
        this.reason = reason;
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Creates a BLOCKED audit entry.
     *
     * @param originalText original text that was blocked
     * @param railName     name of the blocking rail
     * @param reason       reason for blocking
     * @return audit entry
     */
    public static AuditEntry blocked(String originalText, String railName, String reason) {
        return new AuditEntry(Instant.now(), originalText, null, railName, reason, Type.BLOCKED);
    }

    /**
     * Creates a MODIFIED audit entry.
     *
     * @param originalText original text before modification
     * @param resultText   text after modification
     * @param railName     name of the modifying rail
     * @param reason       reason for modification
     * @return audit entry
     */
    public static AuditEntry modified(String originalText, String resultText, String railName, String reason) {
        return new AuditEntry(Instant.now(), originalText, resultText, railName, reason, Type.MODIFIED);
    }

    /** @return when the event occurred */
    public Instant getTimestamp() { return timestamp; }

    /** @return original text (may be null if privacy logging is disabled) */
    public String getOriginalText() { return originalText; }

    /** @return result text (null for BLOCKED entries) */
    public String getResultText() { return resultText; }

    /** @return the name of the rail that produced this entry */
    public String getRailName() { return railName; }

    /** @return reason for the action */
    public String getReason() { return reason; }

    /** @return the type of audit event */
    public Type getType() { return type; }

    @Override
    public String toString() {
        return String.format("AuditEntry{type=%s, rail='%s', reason='%s', timestamp=%s}",
                type, railName, reason, timestamp);
    }
}
