package io.jguardrails.detectors.input.pii;

/**
 * Types of Personally Identifiable Information (PII) that JGuardrails can detect and mask.
 *
 * <p><strong>Ordinal order matters.</strong> {@link io.jguardrails.detectors.input.pii.PiiMasker}
 * iterates entities in ordinal (declaration) order via {@link java.util.EnumSet}. Entities that
 * might overlap with later ones must be declared first so they consume the text before the
 * broader pattern can cause a false positive. Current ordering rationale:</p>
 * <ol>
 *   <li>{@link #EMAIL} — anchored to {@code @}, no conflict with other types.</li>
 *   <li>{@link #CREDIT_CARD} — starts with a known BIN prefix; must precede PHONE.</li>
 *   <li>{@link #IBAN} — {@code CC\d\d …} prefix; must precede PHONE.</li>
 *   <li>{@link #SSN} — 3-2-4 digit format is a proper subset of phone patterns.</li>
 *   <li>{@link #IP_ADDRESS} — IP octets separated by dots can partially match PHONE when
 *       a segment follows a separator that is not a digit or letter.</li>
 *   <li>{@link #PHONE} — broadest numeric pattern; must come after more specific types.</li>
 *   <li>{@link #PASSPORT} — alphanumeric; unlikely to conflict with remaining types.</li>
 *   <li>{@link #DATE_OF_BIRTH} — last: dates can share digit groups with passports.</li>
 * </ol>
 */
public enum PiiEntity {
    /** Email addresses (e.g., {@code user@example.com}). */
    EMAIL,
    /** Credit card numbers (Visa, MasterCard, Amex, Mir) and contextual partial cards
     *  ("card ending XXXX"). Must precede {@link #PHONE}. */
    CREDIT_CARD,
    /** International Bank Account Numbers — must precede {@link #PHONE} to avoid partial matches. */
    IBAN,
    /** US Social Security Numbers — must precede {@link #PHONE} (SSN 3-2-4 format is a subset
     *  of phone patterns). */
    SSN,
    /** IPv4 and IPv6 addresses — must precede {@link #PHONE}: IP octets separated by dots
     *  can match as phone numbers when they follow a non-alphanumeric separator. */
    IP_ADDRESS,
    /** Phone numbers in international and local formats. */
    PHONE,
    /** Passport numbers (RU, US, EU formats). */
    PASSPORT,
    /** Dates of birth in common formats. */
    DATE_OF_BIRTH
}
