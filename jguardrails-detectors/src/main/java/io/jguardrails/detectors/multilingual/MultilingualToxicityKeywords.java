package io.jguardrails.detectors.multilingual;

import io.jguardrails.detectors.config.PatternLoader;

import java.util.List;

/**
 * Toxicity keyword dictionaries for languages not covered by the regex-based
 * {@link io.jguardrails.detectors.output.toxicity.ToxicityPatterns}:
 * Chinese (ZH), Japanese (JA), Arabic (AR), Hindi (HI), Turkish (TR), Korean (KO).
 *
 * <p>Keywords are loaded at class initialisation time from the bundled classpath resource
 * {@link PatternLoader#ML_TOXICITY_RESOURCE} ({@code multilingual-toxicity-keywords.yml}).</p>
 *
 * <h2>Coverage per language</h2>
 * <p>Each language includes three categories:</p>
 * <ol>
 *   <li>Hate and insults — equivalents of "you are an idiot", "I hate you".</li>
 *   <li>Threats — equivalents of "I will kill you", "you will regret this".</li>
 *   <li>Aggressive dismissals — equivalents of "shut up", "get lost".</li>
 * </ol>
 *
 * <p>Single-word terms are included only when they are unambiguously offensive in context.
 * Borderline words that can appear in benign technical text are excluded to minimise
 * false positives.</p>
 */
public final class MultilingualToxicityKeywords {

    private MultilingualToxicityKeywords() {}

    /** Chinese (Simplified) toxicity keywords. */
    public static final List<String> CHINESE  =
            PatternLoader.loadKeywords(PatternLoader.ML_TOXICITY_RESOURCE, "zh");

    /** Japanese toxicity keywords. */
    public static final List<String> JAPANESE =
            PatternLoader.loadKeywords(PatternLoader.ML_TOXICITY_RESOURCE, "ja");

    /** Arabic toxicity keywords. */
    public static final List<String> ARABIC   =
            PatternLoader.loadKeywords(PatternLoader.ML_TOXICITY_RESOURCE, "ar");

    /** Hindi (Devanagari) toxicity keywords. */
    public static final List<String> HINDI    =
            PatternLoader.loadKeywords(PatternLoader.ML_TOXICITY_RESOURCE, "hi");

    /** Turkish toxicity keywords. */
    public static final List<String> TURKISH  =
            PatternLoader.loadKeywords(PatternLoader.ML_TOXICITY_RESOURCE, "tr");

    /** Korean toxicity keywords. */
    public static final List<String> KOREAN   =
            PatternLoader.loadKeywords(PatternLoader.ML_TOXICITY_RESOURCE, "ko");

    /** Combined keyword list for all six supported multilingual locales. */
    public static final List<String> ALL =
            PatternLoader.loadAllKeywords(PatternLoader.ML_TOXICITY_RESOURCE);
}
