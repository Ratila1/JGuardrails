package io.jguardrails.core;

/**
 * The action returned by a rail after processing input or output text.
 *
 * @see RailResult
 */
public enum RailAction {

    /**
     * Allow the text through unchanged.
     */
    PASS,

    /**
     * Block the text — do not forward to LLM (for input) or to user (for output).
     */
    BLOCK,

    /**
     * Replace the text with a modified version (e.g., PII masking, content sanitisation).
     */
    MODIFY
}
