package io.jguardrails.detectors.output.toxicity;

import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import io.jguardrails.detectors.config.PatternLoader;
import io.jguardrails.detectors.engine.CompositePatternEngine;
import io.jguardrails.detectors.engine.KeywordAutomatonEngine;
import io.jguardrails.detectors.engine.MatchedSpec;
import io.jguardrails.detectors.engine.PatternSpec;
import io.jguardrails.detectors.engine.RegexPatternEngine;
import io.jguardrails.detectors.engine.TextPatternEngine;
import io.jguardrails.detectors.multilingual.KeywordMatcher;
import io.jguardrails.detectors.multilingual.MultilingualToxicityKeywords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks LLM output for toxic content before returning it to the user.
 *
 * <p>Detects:</p>
 * <ul>
 *   <li>Profanity / offensive language</li>
 *   <li>Hate speech and discriminatory content</li>
 *   <li>Threats and incitement to violence</li>
 *   <li>Self-harm content</li>
 *   <li>Third-person abuse — insults and death-wishes about absent persons
 *       ("he is an idiot", "she is a waste of space", "they should die")</li>
 * </ul>
 *
 * <p>Internally the checker holds a list of {@link PatternSpec} descriptors and delegates
 * matching to a {@link TextPatternEngine}.  The default engine is {@link RegexPatternEngine};
 * a custom engine can be supplied via {@link Builder#engine(TextPatternEngine)}.</p>
 *
 * <p>Multilingual support (ZH/JA/AR/HI/TR/KO) is enabled by default and uses
 * {@link KeywordMatcher} for scripts where regex {@code \b} word boundaries are
 * undefined. Disable via {@link Builder#multilingualEnabled(boolean)}.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ToxicityChecker checker = ToxicityChecker.builder()
 *     .categories(Category.PROFANITY, Category.HATE_SPEECH)
 *     .build();
 * }</pre>
 *
 * <h2>Custom patterns and keywords</h2>
 * <pre>{@code
 * // Plug in a completely different engine:
 * checker = ToxicityChecker.builder()
 *     .engine(myAhoCorasickEngine)
 *     .build();
 *
 * // Replace default patterns with patterns from a file:
 * checker = ToxicityChecker.builder()
 *     .patternsFromFile(myFile, "custom_section")
 *     .build();
 *
 * // Add patterns on top of defaults:
 * checker = ToxicityChecker.builder()
 *     .addPatternsFromFile(myFile, "extra_section")
 *     .build();
 *
 * // Replace multilingual keywords:
 * checker = ToxicityChecker.builder()
 *     .keywordsFromFile(myKeywordsFile)
 *     .build();
 *
 * // Add keywords on top of defaults:
 * checker = ToxicityChecker.builder()
 *     .addKeywordsFromFile(myKeywordsFile)
 *     .build();
 * }</pre>
 */
public class ToxicityChecker implements OutputRail {

    private static final Logger log = LoggerFactory.getLogger(ToxicityChecker.class);

    /** Toxicity categories that can be individually enabled. */
    public enum Category {
        PROFANITY, HATE_SPEECH, THREATS, SELF_HARM,
        /**
         * Third-person derogatory content: insults about absent persons ("he is an idiot"),
         * dehumanising phrases ("waste of space"), and third-person death wishes
         * ("she should die", "he doesn't deserve to live").
         */
        THIRD_PERSON_ABUSE
    }

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final Set<Category> categories;
    /** Active spec list — controls which patterns the engine is asked to evaluate. */
    private final List<PatternSpec> activeSpecs;
    /** Matching backend — resolves spec ids to actual matching logic. */
    private final TextPatternEngine patternEngine;
    private final boolean multilingualEnabled;
    private final KeywordMatcher multilingualMatcher;

    private ToxicityChecker(Builder builder) {
        this.name               = builder.name;
        this.enabled            = builder.enabled;
        this.priority           = builder.priority;
        this.categories         = EnumSet.copyOf(builder.categories.isEmpty()
                ? EnumSet.allOf(Category.class) : builder.categories);
        this.multilingualEnabled = builder.multilingualEnabled;
        this.activeSpecs        = buildActiveSpecs(builder, this.categories);
        this.patternEngine      = buildEngine(builder);
        this.multilingualMatcher = buildKeywordMatcher(builder);
    }

    // ── Construction helpers ──────────────────────────────────────────────────

    private static List<PatternSpec> buildActiveSpecs(Builder builder, Set<Category> categories) {
        List<PatternSpec> specs = new ArrayList<>();
        if (builder.overrideSpecs != null) {
            specs.addAll(builder.overrideSpecs);
        } else {
            if (categories.contains(Category.PROFANITY))          specs.addAll(ToxicityPatterns.PROFANITY_SPECS);
            if (categories.contains(Category.HATE_SPEECH))        specs.addAll(ToxicityPatterns.HATE_SPEECH_SPECS);
            if (categories.contains(Category.THREATS))            specs.addAll(ToxicityPatterns.THREATS_SPECS);
            if (categories.contains(Category.SELF_HARM))          specs.addAll(ToxicityPatterns.SELF_HARM_SPECS);
            if (categories.contains(Category.THIRD_PERSON_ABUSE)) specs.addAll(ToxicityPatterns.THIRD_PERSON_ABUSE_SPECS);
        }
        // Inline custom blocked words (auto-id)
        for (Map.Entry<String, Pattern> e : builder.customWordEntries) {
            specs.add(new PatternSpec(e.getKey(), "custom"));
        }
        // Extra specs from addPatternsFromFile
        specs.addAll(builder.extraSpecs);
        return List.copyOf(specs);
    }

    private static TextPatternEngine buildEngine(Builder builder) {
        // Fully custom engine: user takes responsibility for spec compatibility
        if (builder.engineOverride != null) {
            return builder.engineOverride;
        }

        // ── Regex sub-engine ──────────────────────────────────────────────────
        RegexPatternEngine.Builder rb = RegexPatternEngine.builder();
        if (builder.overrideEnginePatterns != null) {
            rb.registerAll(builder.overrideEnginePatterns);
        } else {
            rb.registerAll(ToxicityPatterns.DEFAULT_REGEX_ENGINE.patterns());
        }
        for (Map.Entry<String, Pattern> e : builder.customWordEntries) {
            rb.register(e.getKey(), e.getValue());
        }
        rb.registerAll(builder.extraEnginePatterns);
        RegexPatternEngine regexEngine = rb.build();

        // ── Keyword sub-engine ────────────────────────────────────────────────
        Map<String, String> kwMap = new LinkedHashMap<>();
        if (builder.overrideKeywordPatterns != null) {
            kwMap.putAll(builder.overrideKeywordPatterns);
        } else {
            kwMap.putAll(ToxicityPatterns.DEFAULT_KEYWORD_ENGINE.keywords());
        }
        kwMap.putAll(builder.extraKeywordPatterns);
        KeywordAutomatonEngine keywordEngine = new KeywordAutomatonEngine(kwMap);

        return new CompositePatternEngine(regexEngine, keywordEngine);
    }

    private static KeywordMatcher buildKeywordMatcher(Builder builder) {
        List<String> keywords;
        if (builder.overrideKeywords != null) {
            keywords = builder.overrideKeywords;
        } else {
            List<String> combined = new ArrayList<>(MultilingualToxicityKeywords.ALL);
            combined.addAll(builder.extraKeywords);
            keywords = List.copyOf(combined);
        }
        return new KeywordMatcher(keywords);
    }

    // ── OutputRail ────────────────────────────────────────────────────────────

    @Override public String name()       { return name; }
    @Override public int priority()      { return priority; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public RailResult process(String output, String originalInput, RailContext context) {
        Objects.requireNonNull(output, "output must not be null");

        // ── Phase 1: spec-based pattern matching (EN/RU/FR/DE/ES/PL/IT) ────
        // findFirst() dispatches KEYWORD specs to Aho-Corasick and REGEX specs to the regex
        // engine in a single call, providing O(n) keyword matching across all active specs.
        Optional<MatchedSpec> hit = patternEngine.findFirst(output, activeSpecs);
        if (hit.isPresent()) {
            MatchedSpec ms = hit.get();
            log.debug("Toxicity pattern matched: id='{}' category='{}' type='{}'",
                    ms.spec().id(), ms.spec().category(), ms.spec().type());
            return RailResult.block(name(),
                    "Toxic content detected in LLM response: matched pattern '"
                    + ms.result().matchedText() + "'");
        }

        // ── Phase 2: multilingual keyword matching (ZH/JA/AR/HI/TR/KO) ─────
        if (multilingualEnabled) {
            Optional<String> mlHit = multilingualMatcher.firstMatch(output);
            if (mlHit.isPresent()) {
                log.debug("Multilingual toxicity keyword matched: '{}'", mlHit.get());
                return RailResult.block(name(),
                        "Toxic multilingual content detected in LLM response");
            }
        }

        return RailResult.pass(output, name());
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link ToxicityChecker}. */
    public static final class Builder {
        private String name = "toxicity-checker";
        private boolean enabled = true;
        private int priority = 10;
        private boolean multilingualEnabled = true;

        private final Set<Category> categories = new HashSet<>();

        // ── Pattern engine ────────────────────────────────────────────────────
        /** Fully custom engine — overrides all default engine construction. */
        private TextPatternEngine engineOverride = null;

        /** Inline custom blocked words (auto-id generated). */
        private final List<Map.Entry<String, Pattern>> customWordEntries = new ArrayList<>();
        private int customWordCounter = 0;

        /** Override: replace default specs/engine with those from a file. */
        private List<PatternSpec> overrideSpecs = null;
        private Map<String, Pattern> overrideEnginePatterns = null;
        private Map<String, String> overrideKeywordPatterns = null;

        /** Extend: add specs + patterns on top of defaults. */
        private final List<PatternSpec> extraSpecs = new ArrayList<>();
        private final Map<String, Pattern> extraEnginePatterns = new LinkedHashMap<>();
        private final Map<String, String> extraKeywordPatterns = new LinkedHashMap<>();

        // ── Multilingual keywords ──────────────────────────────────────────────
        private List<String> overrideKeywords = null;
        private final List<String> extraKeywords = new ArrayList<>();

        private Builder() {}

        public Builder name(String name)        { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority)   { this.priority = priority; return this; }

        public Builder categories(Category... categories) {
            this.categories.addAll(Arrays.asList(categories));
            return this;
        }

        /**
         * Plugs in a fully custom {@link TextPatternEngine}.  When set, the default
         * {@link RegexPatternEngine} is not constructed; the custom engine is used as-is.
         * The active spec list is still built from category settings — the caller must
         * ensure the engine can handle all spec ids it will be asked to evaluate.
         *
         * @param engine custom engine implementation
         * @return this builder
         */
        public Builder engine(TextPatternEngine engine) {
            this.engineOverride = engine;
            return this;
        }

        /**
         * Adds a custom word that will be blocked (matched as a whole word,
         * case-insensitively). An auto-generated id is assigned.
         *
         * @param word the word to block
         * @return this builder
         */
        public Builder addBlockedWord(String word) {
            String id = "custom_word_" + customWordCounter++;
            customWordEntries.add(Map.entry(id,
                    Pattern.compile("\\b" + Pattern.quote(word) + "\\b",
                            Pattern.CASE_INSENSITIVE)));
            return this;
        }

        /**
         * <strong>Replaces</strong> all default category-based patterns with patterns
         * loaded from {@code sectionKey} in the given YAML file.
         *
         * @param file       path to the YAML file
         * @param sectionKey top-level YAML key to load
         * @return this builder
         */
        public Builder patternsFromFile(Path file, String sectionKey) {
            this.overrideSpecs          = PatternLoader.loadSpecsFromFile(file, sectionKey);
            this.overrideEnginePatterns = PatternLoader.buildRegexEngineFromFile(file, sectionKey).patterns();
            this.overrideKeywordPatterns = PatternLoader.buildKeywordEngineFromFile(file, sectionKey).keywords();
            return this;
        }

        /**
         * <strong>Adds</strong> patterns from the given YAML file on top of the default
         * (or overridden) pattern set.  Supports both {@code type: REGEX} and
         * {@code type: KEYWORD} entries.
         *
         * @param file       path to the YAML file
         * @param sectionKey top-level YAML key to load
         * @return this builder
         */
        public Builder addPatternsFromFile(Path file, String sectionKey) {
            this.extraSpecs.addAll(PatternLoader.loadSpecsFromFile(file, sectionKey));
            this.extraEnginePatterns.putAll(PatternLoader.buildRegexEngineFromFile(file, sectionKey).patterns());
            this.extraKeywordPatterns.putAll(PatternLoader.buildKeywordEngineFromFile(file, sectionKey).keywords());
            return this;
        }

        /**
         * <strong>Replaces</strong> the default multilingual keyword list with all keywords
         * from the given YAML file.
         */
        public Builder keywordsFromFile(Path file) {
            this.overrideKeywords = PatternLoader.loadAllKeywordsFromFile(file);
            return this;
        }

        /**
         * <strong>Adds</strong> keywords from the given YAML file on top of the defaults.
         */
        public Builder addKeywordsFromFile(Path file) {
            this.extraKeywords.addAll(PatternLoader.loadAllKeywordsFromFile(file));
            return this;
        }

        /**
         * Enables or disables multilingual toxicity detection for ZH/JA/AR/HI/TR/KO.
         */
        public Builder multilingualEnabled(boolean enabled) {
            this.multilingualEnabled = enabled;
            return this;
        }

        public ToxicityChecker build() {
            return new ToxicityChecker(this);
        }
    }
}
