package io.jguardrails.detectors.input.pii;

/**
 * Types of Personally Identifiable Information (PII) that JGuardrails can detect and mask.
 */
public enum PiiEntity {
    /** Email addresses (e.g., {@code user@example.com}). */
    EMAIL,
    /** Credit card numbers (Visa, MasterCard, Amex, Mir). */
    CREDIT_CARD,
    /** International Bank Account Numbers — must precede PHONE to avoid partial matches. */
    IBAN,
    /** US Social Security Numbers — must precede PHONE (SSN 3-2-4 format is a subset of phone). */
    SSN,
    /** Phone numbers in international and local formats. */
    PHONE,
    /** Passport numbers (RU, US, EU formats). */
    PASSPORT,
    /** IPv4 and IPv6 addresses. */
    IP_ADDRESS,
    /** Dates of birth in common formats. */
    DATE_OF_BIRTH
}
