package io.jguardrails.detectors.input.jailbreak;

import io.jguardrails.detectors.config.PatternLoader;
import io.jguardrails.detectors.engine.CompositePatternEngine;
import io.jguardrails.detectors.engine.KeywordAutomatonEngine;
import io.jguardrails.detectors.engine.PatternSpec;
import io.jguardrails.detectors.engine.RegexPatternEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Compiled patterns for detecting jailbreak and prompt-injection attacks.
 *
 * <p>Patterns are loaded at class initialisation time from the bundled classpath resource
 * {@link PatternLoader#JAILBREAK_RESOURCE} ({@code jailbreak-patterns.yml}).  They are grouped
 * by sensitivity level; higher levels include all patterns from lower levels plus additional,
 * more aggressive checks.</p>
 *
 * <p>Supported languages: English, Russian, French, German, Spanish, Polish, Italian.</p>
 *
 * <h2>Matching engines</h2>
 * <p>YAML entries with {@code type: REGEX} are stored in {@link #DEFAULT_REGEX_ENGINE}
 * ({@link RegexPatternEngine}).  Entries with {@code type: KEYWORD} are stored in
 * {@link #DEFAULT_KEYWORD_ENGINE} ({@link KeywordAutomatonEngine}).
 * {@link #DEFAULT_ENGINE} is a {@link CompositePatternEngine} that routes each spec to
 * the appropriate sub-engine and is the recommended entry point.</p>
 *
 * <h2>Custom / override patterns</h2>
 * <p>Use {@link PatternLoader#loadPatternsFromFile} and {@link PatternLoader#merge} to combine
 * the defaults with user-supplied patterns, or supply them directly via
 * {@link JailbreakDetector.Builder#addPatternsFromFile} and
 * {@link JailbreakDetector.Builder#patternsFromFile}.</p>
 */
public final class JailbreakPatterns {

    private JailbreakPatterns() {}

    // ── Pattern lists (backward-compatible, REGEX entries only) ──────────────

    /**
     * Patterns always active regardless of sensitivity level.
     * @deprecated Use {@link #HIGH_CONFIDENCE_SPECS} with {@link #DEFAULT_ENGINE}.
     */
    @Deprecated(since = "0.5", forRemoval = false)
    public static final List<Pattern> HIGH_CONFIDENCE =
            PatternLoader.loadPatterns(PatternLoader.JAILBREAK_RESOURCE, "high_confidence");

    /**
     * Additional patterns enabled at MEDIUM sensitivity.
     * @deprecated Use {@link #MEDIUM_CONFIDENCE_SPECS} with {@link #DEFAULT_ENGINE}.
     */
    @Deprecated(since = "0.5", forRemoval = false)
    public static final List<Pattern> MEDIUM_CONFIDENCE =
            PatternLoader.loadPatterns(PatternLoader.JAILBREAK_RESOURCE, "medium_confidence");

    /**
     * Additional patterns enabled at HIGH sensitivity (broader, may have false positives).
     * @deprecated Use {@link #LOW_CONFIDENCE_SPECS} with {@link #DEFAULT_ENGINE}.
     */
    @Deprecated(since = "0.5", forRemoval = false)
    public static final List<Pattern> LOW_CONFIDENCE =
            PatternLoader.loadPatterns(PatternLoader.JAILBREAK_RESOURCE, "low_confidence");

    // ── PatternSpec lists (includes both REGEX and KEYWORD specs) ─────────────

    /** {@link PatternSpec} descriptors (id + category + type) for the high-confidence section. */
    public static final List<PatternSpec> HIGH_CONFIDENCE_SPECS =
            PatternLoader.loadSpecs(PatternLoader.JAILBREAK_RESOURCE, "high_confidence");

    /** {@link PatternSpec} descriptors for the medium-confidence section. */
    public static final List<PatternSpec> MEDIUM_CONFIDENCE_SPECS =
            PatternLoader.loadSpecs(PatternLoader.JAILBREAK_RESOURCE, "medium_confidence");

    /** {@link PatternSpec} descriptors for the low-confidence section. */
    public static final List<PatternSpec> LOW_CONFIDENCE_SPECS =
            PatternLoader.loadSpecs(PatternLoader.JAILBREAK_RESOURCE, "low_confidence");

    // ── Default engines ───────────────────────────────────────────────────────

    /**
     * {@link RegexPatternEngine} pre-populated with all {@code type: REGEX} jailbreak patterns
     * from all three sensitivity sections.
     */
    public static final RegexPatternEngine DEFAULT_REGEX_ENGINE =
            PatternLoader.buildRegexEngine(PatternLoader.JAILBREAK_RESOURCE,
                    "high_confidence", "medium_confidence", "low_confidence");

    /**
     * {@link KeywordAutomatonEngine} pre-populated with all {@code type: KEYWORD} jailbreak
     * phrases from all three sensitivity sections.  Uses Aho-Corasick for O(n) matching.
     */
    public static final KeywordAutomatonEngine DEFAULT_KEYWORD_ENGINE =
            PatternLoader.buildKeywordEngine(PatternLoader.JAILBREAK_RESOURCE,
                    "high_confidence", "medium_confidence", "low_confidence");

    /**
     * Default {@link CompositePatternEngine} that combines {@link #DEFAULT_REGEX_ENGINE} and
     * {@link #DEFAULT_KEYWORD_ENGINE}, routing each {@link PatternSpec} to the appropriate
     * sub-engine based on its {@link PatternSpec.Type}.
     *
     * <p>This is the recommended engine for {@link JailbreakDetector} — it provides O(n)
     * keyword matching for {@code KEYWORD} specs and full regex power for {@code REGEX} specs.</p>
     */
    public static final CompositePatternEngine DEFAULT_ENGINE =
            new CompositePatternEngine(DEFAULT_REGEX_ENGINE, DEFAULT_KEYWORD_ENGINE);

    // ── Sensitivity dispatch ──────────────────────────────────────────────────

    /**
     * Returns the effective {@link PatternSpec} list for the given sensitivity level.
     *
     * <ul>
     *   <li>{@code LOW}    → high-confidence specs only</li>
     *   <li>{@code MEDIUM} → high + medium specs</li>
     *   <li>{@code HIGH}   → all three spec lists</li>
     * </ul>
     *
     * @param sensitivity the requested sensitivity level
     * @return immutable merged spec list (includes both REGEX and KEYWORD specs)
     */
    public static List<PatternSpec> specsForSensitivity(JailbreakDetector.Sensitivity sensitivity) {
        return switch (sensitivity) {
            case HIGH   -> concatLists(HIGH_CONFIDENCE_SPECS, MEDIUM_CONFIDENCE_SPECS, LOW_CONFIDENCE_SPECS);
            case MEDIUM -> concatLists(HIGH_CONFIDENCE_SPECS, MEDIUM_CONFIDENCE_SPECS);
            case LOW    -> HIGH_CONFIDENCE_SPECS;
        };
    }

    /**
     * Returns the effective compiled {@link Pattern} list for the given sensitivity level.
     *
     * @param sensitivity the requested sensitivity level
     * @return immutable merged pattern list
     * @deprecated Prefer {@link #specsForSensitivity(JailbreakDetector.Sensitivity)} with
     *             {@link #DEFAULT_ENGINE}.
     */
    @Deprecated(since = "0.4", forRemoval = false)
    public static List<Pattern> forSensitivity(JailbreakDetector.Sensitivity sensitivity) {
        return switch (sensitivity) {
            case HIGH   -> concatLists(HIGH_CONFIDENCE, MEDIUM_CONFIDENCE, LOW_CONFIDENCE);
            case MEDIUM -> concatLists(HIGH_CONFIDENCE, MEDIUM_CONFIDENCE);
            case LOW    -> HIGH_CONFIDENCE;
        };
    }

    @SafeVarargs
    private static <T> List<T> concatLists(List<T>... lists) {
        ArrayList<T> result = new ArrayList<>();
        for (List<T> list : lists) result.addAll(list);
        return List.copyOf(result);
    }
}
