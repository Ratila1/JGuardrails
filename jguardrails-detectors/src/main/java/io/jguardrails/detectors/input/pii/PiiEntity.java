package io.jguardrails.detectors.input.pii;

/**
 * Types of Personally Identifiable Information (PII) that JGuardrails can detect and mask.
 */
public enum PiiEntity {
    /** Email addresses (e.g., {@code user@example.com}). */
    EMAIL,
    /** Credit card numbers (Visa, MasterCard, Amex, Mir). */
    CREDIT_CARD,
    /** Phone numbers in international and local formats. */
    PHONE,
    /** US Social Security Numbers. */
    SSN,
    /** Passport numbers (RU, US, EU formats). */
    PASSPORT,
    /** IPv4 and IPv6 addresses. */
    IP_ADDRESS,
    /** International Bank Account Numbers. */
    IBAN,
    /** Dates of birth in common formats. */
    DATE_OF_BIRTH
}
