package io.jguardrails.detectors.output.toxicity;

import io.jguardrails.detectors.config.PatternLoader;
import io.jguardrails.detectors.engine.CompositePatternEngine;
import io.jguardrails.detectors.engine.KeywordAutomatonEngine;
import io.jguardrails.detectors.engine.PatternSpec;
import io.jguardrails.detectors.engine.RegexPatternEngine;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Patterns used by {@link ToxicityChecker} to detect toxic content.
 *
 * <p>Patterns are loaded at class initialisation time from the bundled classpath resource
 * {@link PatternLoader#TOXICITY_RESOURCE} ({@code toxicity-patterns.yml}).
 * Supported languages: English, Russian, French, German, Spanish, Polish, Italian.</p>
 *
 * <h2>Matching engines</h2>
 * <p>YAML entries with {@code type: REGEX} (the default) go to {@link #DEFAULT_REGEX_ENGINE};
 * entries with {@code type: KEYWORD} go to {@link #DEFAULT_KEYWORD_ENGINE}.
 * {@link #DEFAULT_ENGINE} is a {@link CompositePatternEngine} that combines both.</p>
 *
 * <h2>Third-person toxicity design notes</h2>
 * <p>{@link #THIRD_PERSON_ABUSE} covers constructs of the form
 * <em>subject + copula + insult</em> and <em>subject + death-wish verb phrase</em>,
 * where the subject is a third-person pronoun or a "this/that person" reference.
 * The patterns are deliberately scoped to these human-referencing subjects so that
 * sentences like "this library is useless" or "the process should die" are not caught.
 * Abstract narrative phrasing such as "the villain threatens to kill" (no copula + insult)
 * or "the character is evil" (evil is not an insult slur) passes through cleanly.</p>
 *
 * <p>All third-person patterns use {@code UC}
 * ({@code CASE_INSENSITIVE | UNICODE_CASE | UNICODE_CHARACTER_CLASS}) so that {@code \b}
 * and {@code \w} are Unicode-aware — required for insult words ending in non-ASCII letters
 * (Polish {@code ą}, French {@code è}, Cyrillic, etc.).</p>
 *
 * <p>The modifier slot {@code (?:\w+\s+){0,2}} absorbs up to two optional intensifier or
 * quantifier words between the copula and the insult, e.g. "she is completely worthless",
 * "they are all morons", "he is a complete idiot".</p>
 *
 * <h2>Custom / override patterns</h2>
 * <p>Use {@link PatternLoader#loadPatternsFromFile} and {@link PatternLoader#merge} to combine
 * the defaults with user-supplied patterns, or supply them directly via
 * {@link ToxicityChecker.Builder#addPatternsFromFile} and
 * {@link ToxicityChecker.Builder#patternsFromFile}.</p>
 */
public final class ToxicityPatterns {

    private ToxicityPatterns() {}

    // ── Pattern lists (backward-compatible, REGEX entries only) ──────────────

    /** Common profanity patterns (EN / RU / FR / DE / ES / PL / IT). */
    public static final List<Pattern> PROFANITY =
            PatternLoader.loadPatterns(PatternLoader.TOXICITY_RESOURCE, "profanity");

    /** Hate speech and discriminatory language patterns. */
    public static final List<Pattern> HATE_SPEECH =
            PatternLoader.loadPatterns(PatternLoader.TOXICITY_RESOURCE, "hate_speech");

    /** Threat and violence incitement patterns. */
    public static final List<Pattern> THREATS =
            PatternLoader.loadPatterns(PatternLoader.TOXICITY_RESOURCE, "threats");

    /**
     * Third-person abuse patterns: derogatory statements about absent persons.
     *
     * <h3>Scope</h3>
     * <ul>
     *   <li><em>pronoun/reference + copula + insult adjective/noun</em>:
     *       "he is an idiot", "she is worthless", "that guy is pathetic"</li>
     *   <li><em>dehumanising noun phrases</em>:
     *       "waste of space/time/oxygen", "not worth anything"</li>
     *   <li><em>third-person death wishes</em>:
     *       "she should die", "he deserves to die", "they don't deserve to live"</li>
     * </ul>
     *
     * <h3>False-positive guard</h3>
     * <p>Subjects are restricted to human-referencing pronouns and "this/that person/guy/girl"
     * so that sentences like "this system is useless", "the process should die" or
     * abstract narrative phrases ("the villain is evil") do not trigger.</p>
     */
    public static final List<Pattern> THIRD_PERSON_ABUSE =
            PatternLoader.loadPatterns(PatternLoader.TOXICITY_RESOURCE, "third_person_abuse");

    /** Self-harm and crisis content patterns. */
    public static final List<Pattern> SELF_HARM =
            PatternLoader.loadPatterns(PatternLoader.TOXICITY_RESOURCE, "self_harm");

    // ── PatternSpec lists (includes both REGEX and KEYWORD specs) ─────────────

    /** {@link PatternSpec} descriptors for the profanity section. */
    public static final List<PatternSpec> PROFANITY_SPECS =
            PatternLoader.loadSpecs(PatternLoader.TOXICITY_RESOURCE, "profanity");

    /** {@link PatternSpec} descriptors for the hate-speech section. */
    public static final List<PatternSpec> HATE_SPEECH_SPECS =
            PatternLoader.loadSpecs(PatternLoader.TOXICITY_RESOURCE, "hate_speech");

    /** {@link PatternSpec} descriptors for the threats section. */
    public static final List<PatternSpec> THREATS_SPECS =
            PatternLoader.loadSpecs(PatternLoader.TOXICITY_RESOURCE, "threats");

    /** {@link PatternSpec} descriptors for the third-person-abuse section. */
    public static final List<PatternSpec> THIRD_PERSON_ABUSE_SPECS =
            PatternLoader.loadSpecs(PatternLoader.TOXICITY_RESOURCE, "third_person_abuse");

    /** {@link PatternSpec} descriptors for the self-harm section. */
    public static final List<PatternSpec> SELF_HARM_SPECS =
            PatternLoader.loadSpecs(PatternLoader.TOXICITY_RESOURCE, "self_harm");

    // ── Default engines ───────────────────────────────────────────────────────

    /**
     * {@link RegexPatternEngine} pre-populated with all {@code type: REGEX} toxicity patterns
     * from all five categories.  Used by {@link ToxicityChecker} when a pure regex engine
     * is needed.
     */
    public static final RegexPatternEngine DEFAULT_REGEX_ENGINE =
            PatternLoader.buildRegexEngine(PatternLoader.TOXICITY_RESOURCE,
                    "profanity", "hate_speech", "threats", "self_harm", "third_person_abuse");

    /**
     * {@link KeywordAutomatonEngine} pre-populated with all {@code type: KEYWORD} toxicity
     * phrases from all five categories.  Uses Aho-Corasick for O(n) matching.
     */
    public static final KeywordAutomatonEngine DEFAULT_KEYWORD_ENGINE =
            PatternLoader.buildKeywordEngine(PatternLoader.TOXICITY_RESOURCE,
                    "profanity", "hate_speech", "threats", "self_harm", "third_person_abuse");

    /**
     * Default {@link CompositePatternEngine} combining {@link #DEFAULT_REGEX_ENGINE} and
     * {@link #DEFAULT_KEYWORD_ENGINE}.  Routes each {@link PatternSpec} to the correct
     * sub-engine based on its {@link PatternSpec.Type}.  Recommended engine for
     * {@link ToxicityChecker}.
     */
    public static final CompositePatternEngine DEFAULT_ENGINE =
            new CompositePatternEngine(DEFAULT_REGEX_ENGINE, DEFAULT_KEYWORD_ENGINE);
}
