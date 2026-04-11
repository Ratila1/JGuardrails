package io.jguardrails.detectors.input.pii;

/**
 * Strategy for masking detected PII entities.
 */
public enum PiiMaskingStrategy {
    /**
     * Fully replaces the PII with a placeholder (e.g., {@code [EMAIL REDACTED]}).
     * Default strategy.
     */
    REDACT,

    /**
     * Partially masks the PII while preserving some structure
     * (e.g., email: {@code j***@e***.com}, phone: {@code +7***1234}).
     */
    MASK_PARTIAL,

    /**
     * Replaces PII with its SHA-256 hash in hexadecimal.
     * Allows consistent de-identified matching without storing originals.
     */
    HASH
}
