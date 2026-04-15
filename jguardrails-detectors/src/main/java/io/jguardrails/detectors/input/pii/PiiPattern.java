package io.jguardrails.detectors.input.pii;

import io.jguardrails.detectors.config.PatternLoader;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Compiled regex patterns for each supported {@link PiiEntity} type.
 *
 * <p>Patterns are loaded at class initialisation time from the bundled classpath resource
 * {@link PatternLoader#PII_RESOURCE} ({@code pii-patterns.yml}).</p>
 *
 * <h2>Design notes — why PII patterns do NOT use {@link io.jguardrails.normalize.TextNormalizer}</h2>
 *
 * <p>{@link io.jguardrails.normalize.DefaultTextNormalizer} is intentionally <em>not</em> applied
 * before PII detection. Here is why:</p>
 * <ul>
 *   <li><strong>Symbol destruction</strong>: the normalizer maps {@code @→a} and {@code $→s}.
 *       Email patterns depend on the literal {@code @} character; normalizing before detection
 *       would break email matching entirely.</li>
 *   <li><strong>Length / offset drift</strong>: leet-folding and whitespace collapse change the
 *       length of the string. The masker replaces matched spans in the <em>original</em> text
 *       using {@link java.util.regex.Matcher#appendReplacement}. If detection ran on a normalized
 *       copy, the match offsets would not correspond to the original positions, making correct
 *       replacement impossible without expensive index remapping.</li>
 *   <li><strong>Format destruction</strong>: phone and credit-card patterns rely on specific
 *       separator characters ({@code +-.()} and {@code -}) and digit groupings that normalization
 *       collapses or alters.</li>
 *   <li><strong>Predictability</strong>: PII masking is a high-stakes operation — false negatives
 *       (missed PII) and false positives (incorrectly masked text) both have real consequences.
 *       Keeping detection on the original, unmodified text maximises pattern precision and
 *       makes behaviour easy to reason about and audit.</li>
 * </ul>
 *
 * <p>If future requirements demand catching leet-obfuscated PII (e.g., {@code j0hn@example.com}),
 * the recommended approach is a second detection pass on the normalised text with offset remapping,
 * implemented as a separate, opt-in component rather than changing the default pipeline.</p>
 *
 * <h2>Pattern ordering</h2>
 * <p>See {@link PiiEntity} for the rationale behind entity processing order.</p>
 */
public final class PiiPattern {

    private PiiPattern() {}

    // ── Patterns loaded from YAML ─────────────────────────────────────────────

    private static final Map<String, Pattern> YAML =
            PatternLoader.loadPiiPatterns(PatternLoader.PII_RESOURCE);

    // ── Per-entity constants (backward-compatible public API) ─────────────────

    /** Email address pattern. */
    public static final Pattern EMAIL         = YAML.get("EMAIL");

    /**
     * Credit card number pattern (Visa, MasterCard, Amex, Mir — with optional spaces/dashes).
     *
     * <p>Two sub-patterns combined with {@code |}:</p>
     * <ol>
     *   <li><strong>Full card number</strong> — BIN-prefix validated; lookbehind prevents UUID
     *       false positives; last group {@code {3,4}} covers 15-digit Amex.</li>
     *   <li><strong>Contextual partial card</strong> — {@code "card ending NNNN"}.</li>
     * </ol>
     */
    public static final Pattern CREDIT_CARD   = YAML.get("CREDIT_CARD");

    /** IBAN — compact and space-formatted. */
    public static final Pattern IBAN          = YAML.get("IBAN");

    /**
     * US Social Security Number (XXX-XX-XXXX or XXX XX XXXX).
     * Excludes invalid groups: 000, 666, 9xx (ITIN), and all-zero components.
     */
    public static final Pattern SSN           = YAML.get("SSN");

    /**
     * IPv4 (strict per-octet 0–255) and IPv6 (full 8-group colon-hex).
     * Compressed IPv6 forms (e.g. {@code ::1}) are not matched.
     */
    public static final Pattern IP_ADDRESS    = YAML.get("IP_ADDRESS");

    /**
     * Phone numbers — international and local formats, 7–15 digits.
     * Guards against dates, IPv4 fragments, version numbers, and credit-card sequences.
     */
    public static final Pattern PHONE         = YAML.get("PHONE");

    /** Passport numbers — common cross-country format (1–2 letters + 6–9 digits)
     *  and Russian internal passport (2 digits + space + 7 digits). */
    public static final Pattern PASSPORT      = YAML.get("PASSPORT");

    /** Date of birth in common formats: DD.MM.YYYY, MM/DD/YYYY, YYYY-MM-DD. */
    public static final Pattern DATE_OF_BIRTH = YAML.get("DATE_OF_BIRTH");

    // ── Lookup map ────────────────────────────────────────────────────────────

    /** Lookup map from entity type to compiled pattern. */
    public static final Map<PiiEntity, Pattern> PATTERNS = Map.of(
        PiiEntity.EMAIL,         EMAIL,
        PiiEntity.PHONE,         PHONE,
        PiiEntity.CREDIT_CARD,   CREDIT_CARD,
        PiiEntity.SSN,           SSN,
        PiiEntity.PASSPORT,      PASSPORT,
        PiiEntity.IP_ADDRESS,    IP_ADDRESS,
        PiiEntity.IBAN,          IBAN,
        PiiEntity.DATE_OF_BIRTH, DATE_OF_BIRTH
    );

    /**
     * Returns the pattern for a given entity type.
     *
     * @param entity PII entity type
     * @return compiled pattern
     * @throws IllegalArgumentException if no pattern is registered for the given entity
     */
    public static Pattern forEntity(PiiEntity entity) {
        Pattern pattern = PATTERNS.get(entity);
        if (pattern == null) {
            throw new IllegalArgumentException("No pattern registered for PiiEntity: " + entity);
        }
        return pattern;
    }
}
