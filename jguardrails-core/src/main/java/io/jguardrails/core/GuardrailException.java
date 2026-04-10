package io.jguardrails.core;

/**
 * Base exception for all JGuardrails errors.
 *
 * <p>Subclasses represent specific failure modes such as configuration errors,
 * rail execution failures, or blocked requests.</p>
 */
public class GuardrailException extends RuntimeException {

    /**
     * Constructs a new exception with a message.
     *
     * @param message error description
     */
    public GuardrailException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a message and cause.
     *
     * @param message error description
     * @param cause   underlying exception
     */
    public GuardrailException(String message, Throwable cause) {
        super(message, cause);
    }
}
