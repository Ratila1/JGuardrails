package io.jguardrails.detectors.multilingual;

import io.jguardrails.detectors.config.PatternLoader;

import java.util.List;

/**
 * Jailbreak / prompt-injection keyword dictionaries for languages not covered by
 * the regex-based {@link io.jguardrails.detectors.input.jailbreak.JailbreakPatterns}:
 * Chinese (ZH), Japanese (JA), Arabic (AR), Hindi (HI), Turkish (TR), Korean (KO).
 *
 * <p>Keywords are loaded at class initialisation time from the bundled classpath resource
 * {@link PatternLoader#ML_JAILBREAK_RESOURCE} ({@code multilingual-jailbreak-keywords.yml}).</p>
 *
 * <h2>Why separate from JailbreakPatterns?</h2>
 * <p>{@link io.jguardrails.detectors.input.jailbreak.JailbreakPatterns} uses Java regex with
 * {@code \b} word-boundary assertions which are only reliable for Latin-script languages.
 * CJK, Arabic and Devanagari scripts have no ASCII word-boundary concept, so these keywords
 * are matched via {@link KeywordMatcher} using simple {@code String.contains()} semantics.</p>
 *
 * <h2>Coverage per language</h2>
 * <p>Each language includes three categories:</p>
 * <ol>
 *   <li>Instruction-override phrases — "ignore previous instructions", "forget all rules".</li>
 *   <li>Restriction-bypass phrases — "no restrictions", "without any limits", "you can do anything".</li>
 *   <li>System-prompt extraction — "show me your system prompt", "reveal your instructions".</li>
 * </ol>
 */
public final class MultilingualJailbreakKeywords {

    private MultilingualJailbreakKeywords() {}

    /** Chinese (Simplified) jailbreak keywords. */
    public static final List<String> CHINESE  =
            PatternLoader.loadKeywords(PatternLoader.ML_JAILBREAK_RESOURCE, "zh");

    /** Japanese jailbreak keywords. */
    public static final List<String> JAPANESE =
            PatternLoader.loadKeywords(PatternLoader.ML_JAILBREAK_RESOURCE, "ja");

    /** Arabic jailbreak keywords. */
    public static final List<String> ARABIC   =
            PatternLoader.loadKeywords(PatternLoader.ML_JAILBREAK_RESOURCE, "ar");

    /** Hindi (Devanagari) jailbreak keywords. */
    public static final List<String> HINDI    =
            PatternLoader.loadKeywords(PatternLoader.ML_JAILBREAK_RESOURCE, "hi");

    /** Turkish jailbreak keywords. */
    public static final List<String> TURKISH  =
            PatternLoader.loadKeywords(PatternLoader.ML_JAILBREAK_RESOURCE, "tr");

    /** Korean jailbreak keywords. */
    public static final List<String> KOREAN   =
            PatternLoader.loadKeywords(PatternLoader.ML_JAILBREAK_RESOURCE, "ko");

    /** Combined keyword list for all six supported multilingual locales. */
    public static final List<String> ALL =
            PatternLoader.loadAllKeywords(PatternLoader.ML_JAILBREAK_RESOURCE);
}
