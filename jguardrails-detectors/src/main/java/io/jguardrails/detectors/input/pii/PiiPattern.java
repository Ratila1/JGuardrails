package io.jguardrails.detectors.input.pii;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Compiled regex patterns for each supported {@link PiiEntity} type.
 */
public final class PiiPattern {

    private PiiPattern() {}

    /** Email address pattern. */
    public static final Pattern EMAIL = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    );

    /** Phone number pattern (international and local formats). */
    public static final Pattern PHONE = Pattern.compile(
        "(?:\\+?\\d[\\s\\-.]?){7,15}\\d"
    );

    /** Credit card pattern (Visa, MasterCard, Amex, Mir — with optional spaces/dashes). */
    public static final Pattern CREDIT_CARD = Pattern.compile(
        "(?:4[0-9]{3}|5[1-5][0-9]{2}|2[2-7][0-9]{2}|3[47][0-9]{2}|2200)[\\s\\-]?[0-9]{4}[\\s\\-]?[0-9]{4}[\\s\\-]?[0-9]{0,4}"
    );

    /** US Social Security Number (XXX-XX-XXXX). */
    public static final Pattern SSN = Pattern.compile(
        "\\b(?!000|666|9\\d{2})\\d{3}[\\s\\-](?!00)\\d{2}[\\s\\-](?!0000)\\d{4}\\b"
    );

    /** Passport numbers (simplified cross-country). */
    public static final Pattern PASSPORT = Pattern.compile(
        "\\b(?:[A-Z]{1,2}[\\s\\-]?\\d{6,9}|\\d{2}\\s?\\d{7})\\b"
    );

    /** IPv4 and IPv6 address patterns. */
    public static final Pattern IP_ADDRESS = Pattern.compile(
        "(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)" +
        "|(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
    );

    /** IBAN (International Bank Account Number). */
    public static final Pattern IBAN = Pattern.compile(
        "\\b[A-Z]{2}\\d{2}[A-Z0-9]{4}\\d{7}(?:[A-Z0-9]{0,16})\\b"
    );

    /** Date of birth in common formats (DD.MM.YYYY, MM/DD/YYYY, YYYY-MM-DD). */
    public static final Pattern DATE_OF_BIRTH = Pattern.compile(
        "\\b(?:\\d{1,2}[./\\-]\\d{1,2}[./\\-]\\d{2,4}|\\d{4}[./\\-]\\d{2}[./\\-]\\d{2})\\b"
    );

    /** Lookup map from entity type to compiled pattern. */
    public static final Map<PiiEntity, Pattern> PATTERNS = Map.of(
        PiiEntity.EMAIL, EMAIL,
        PiiEntity.PHONE, PHONE,
        PiiEntity.CREDIT_CARD, CREDIT_CARD,
        PiiEntity.SSN, SSN,
        PiiEntity.PASSPORT, PASSPORT,
        PiiEntity.IP_ADDRESS, IP_ADDRESS,
        PiiEntity.IBAN, IBAN,
        PiiEntity.DATE_OF_BIRTH, DATE_OF_BIRTH
    );

    /**
     * Returns the pattern for a given entity type.
     *
     * @param entity PII entity type
     * @return compiled pattern
     */
    public static Pattern forEntity(PiiEntity entity) {
        Pattern pattern = PATTERNS.get(entity);
        if (pattern == null) {
            throw new IllegalArgumentException("No pattern registered for PiiEntity: " + entity);
        }
        return pattern;
    }
}
