package io.jguardrails.detectors.input.jailbreak;

import io.jguardrails.core.InputRail;
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
import io.jguardrails.detectors.multilingual.MultilingualJailbreakKeywords;
import io.jguardrails.normalize.DefaultTextNormalizer;
import io.jguardrails.normalize.TextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects prompt-injection and jailbreak attempts in user input.
 *
 * <p>Supports two detection modes:</p>
 * <ul>
 *   <li>{@link Mode#PATTERN} — fast regex/keyword matching with no external calls</li>
 *   <li>{@link Mode#HYBRID} — pattern first; if uncertain, escalate to LLM-as-judge</li>
 * </ul>
 *
 * <p>Internally the detector holds a list of {@link PatternSpec} descriptors and delegates
 * matching to a {@link TextPatternEngine}.  The default engine is {@link RegexPatternEngine};
 * a custom engine (Aho-Corasick, ML-backed, etc.) can be supplied via
 * {@link Builder#engine(TextPatternEngine)}.</p>
 *
 * <p>Multilingual support (ZH/JA/AR/HI/TR/KO) is enabled by default and uses
 * {@link KeywordMatcher} — a substring-based engine that works correctly for scripts
 * where Java regex {@code \b} word boundaries are undefined. Disable via
 * {@link Builder#multilingualEnabled(boolean)}.</p>
 *
 * <p>Create via builder:</p>
 * <pre>{@code
 * JailbreakDetector detector = JailbreakDetector.builder()
 *     .sensitivity(Sensitivity.HIGH)
 *     .build();
 * }</pre>
 *
 * <h2>Custom patterns and keywords</h2>
 * <pre>{@code
 * // Plug in a completely different engine:
 * detector = JailbreakDetector.builder()
 *     .engine(myAhoCorasickEngine)
 *     .build();
 *
 * // Replace default patterns with patterns from a file:
 * detector = JailbreakDetector.builder()
 *     .patternsFromFile(myFile, "my_section")
 *     .build();
 *
 * // Add patterns on top of defaults:
 * detector = JailbreakDetector.builder()
 *     .addPatternsFromFile(myFile, "extra_section")
 *     .build();
 * }</pre>
 */
public class JailbreakDetector implements InputRail {

    private static final Logger log = LoggerFactory.getLogger(JailbreakDetector.class);

    /** Detection mode. */
    public enum Mode {
        /** Fast regex/keyword-based detection only. */
        PATTERN,
        /** Use LLM as judge only. */
        LLM_JUDGE,
        /** Pattern first; escalate borderline cases to LLM. */
        HYBRID
    }

    /** Sensitivity level controlling the breadth of pattern matching. */
    public enum Sensitivity {
        /** Most precise — only obvious jailbreak signatures. */
        LOW,
        /** Balanced (default). */
        MEDIUM,
        /** Broadest coverage — may have some false positives. */
        HIGH
    }

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final Mode mode;
    private final Sensitivity sensitivity;
    /** Active spec list — controls which patterns the engine is asked to evaluate. */
    private final List<PatternSpec> activeSpecs;
    /** Matching backend — resolves spec ids to actual matching logic. */
    private final TextPatternEngine patternEngine;
    private final boolean multilingualEnabled;
    private final KeywordMatcher multilingualMatcher;

    private JailbreakDetector(Builder builder) {
        this.name               = builder.name;
        this.enabled            = builder.enabled;
        this.priority           = builder.priority;
        this.mode               = builder.mode;
        this.sensitivity        = builder.sensitivity;
        this.multilingualEnabled = builder.multilingualEnabled;
        this.activeSpecs        = buildActiveSpecs(builder);
        this.patternEngine      = buildEngine(builder);
        this.multilingualMatcher = buildKeywordMatcher(builder);
    }

    // ── Construction helpers ──────────────────────────────────────────────────

    private static List<PatternSpec> buildActiveSpecs(Builder builder) {
        List<PatternSpec> specs = new ArrayList<>();
        if (builder.overrideSpecs != null) {
            specs.addAll(builder.overrideSpecs);
        } else {
            specs.addAll(JailbreakPatterns.specsForSensitivity(builder.sensitivity));
        }
        // Inline custom specs (auto-id)
        for (Map.Entry<String, Pattern> e : builder.customPatternEntries) {
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
            rb.registerAll(JailbreakPatterns.DEFAULT_REGEX_ENGINE.patterns());
        }
        for (Map.Entry<String, Pattern> e : builder.customPatternEntries) {
            rb.register(e.getKey(), e.getValue());
        }
        rb.registerAll(builder.extraEnginePatterns);
        RegexPatternEngine regexEngine = rb.build();

        // ── Keyword sub-engine ────────────────────────────────────────────────
        Map<String, String> kwMap = new LinkedHashMap<>();
        if (builder.overrideKeywordPatterns != null) {
            kwMap.putAll(builder.overrideKeywordPatterns);
        } else {
            kwMap.putAll(JailbreakPatterns.DEFAULT_KEYWORD_ENGINE.keywords());
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
            List<String> combined = new ArrayList<>(MultilingualJailbreakKeywords.ALL);
            combined.addAll(builder.extraKeywords);
            keywords = List.copyOf(combined);
        }
        return new KeywordMatcher(keywords);
    }

    // ── InputRail ─────────────────────────────────────────────────────────────

    @Override public String name()      { return name; }
    @Override public int priority()     { return priority; }
    @Override public boolean isEnabled() { return enabled; }

    private static final TextNormalizer FALLBACK_NORMALIZER = new DefaultTextNormalizer();

    @Override
    public RailResult process(String input, RailContext context) {
        Objects.requireNonNull(input,   "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (mode == Mode.LLM_JUDGE) {
            log.warn("LLM_JUDGE mode requires an LlmClient; falling back to PATTERN mode");
        }

        String normalizedInput = context.getAttribute(TextNormalizer.CONTEXT_KEY, String.class)
                .orElseGet(() -> FALLBACK_NORMALIZER.normalize(input));

        return detectWithPatterns(normalizedInput, input);
    }

    private RailResult detectWithPatterns(String normalizedInput, String originalInput) {
        // ── Phase 1: spec-based pattern matching on normalized candidates ────
        // findFirst() dispatches KEYWORD specs to Aho-Corasick and REGEX specs to the regex
        // engine in a single call, providing O(n) keyword matching across all active specs.
        for (String candidate : buildCandidates(normalizedInput, originalInput)) {
            Optional<MatchedSpec> hit = patternEngine.findFirst(candidate, activeSpecs);
            if (hit.isPresent()) {
                MatchedSpec ms = hit.get();
                log.debug("Jailbreak pattern matched: id='{}' category='{}' type='{}'",
                        ms.spec().id(), ms.spec().category(), ms.spec().type());
                return RailResult.block(
                        name(),
                        "Prompt injection detected: matched pattern '" + ms.result().matchedText() + "'",
                        1.0
                );
            }
        }

        // ── Phase 2: multilingual keyword matching (ZH/JA/AR/HI/TR/KO) ─────
        if (multilingualEnabled) {
            Optional<String> hit = multilingualMatcher.firstMatch(originalInput);
            if (hit.isEmpty()) hit = multilingualMatcher.firstMatch(normalizedInput);
            if (hit.isPresent()) {
                log.debug("Multilingual jailbreak keyword matched: '{}'", hit.get());
                return RailResult.block(
                        name(),
                        "Prompt injection detected: multilingual keyword '" + hit.get() + "'",
                        1.0
                );
            }
        }

        return RailResult.pass(originalInput, name());
    }

    // ── Candidate generation ──────────────────────────────────────────────────

    private static String[] buildCandidates(String normalizedInput, String originalInput) {
        List<String> candidates = new ArrayList<>();

        String s = normalizedInput;
        s = s.replaceAll("(?<=[a-z])\\.", "");          // dotted-acronym collapse
        s = s.replaceAll("(?<=[a-z])-(?=[a-z])", "");  // intra-word hyphen removal
        candidates.add(s);

        String spaced = collapseSpacedLetters(s);
        if (!spaced.equals(s)) candidates.add(spaced);  // spaced-letter collapse

        String raw = originalInput.trim();
        candidates.add(rot13(raw));
        candidates.add(new StringBuilder(raw).reverse().toString());

        String hex = tryHexDecode(raw);
        if (hex != null) candidates.add(hex);

        String b64 = tryBase64Decode(raw);
        if (b64 != null) candidates.add(b64);

        return candidates.toArray(new String[0]);
    }

    private static String collapseSpacedLetters(String s) {
        Matcher m = Pattern.compile("(?<!\\w)(?:[a-zA-Z] ){2,}[a-zA-Z](?!\\w)").matcher(s);
        if (!m.find()) return s;
        m.reset();
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String word = m.group().replace(" ", "")
                    .replaceAll("(?<=[A-Z])(?=[a-z])", " ");
            m.appendReplacement(result, Matcher.quoteReplacement(word));
        }
        m.appendTail(result);
        return result.toString().replaceAll("\\s{2,}", " ").trim();
    }

    private static String rot13(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if      (c >= 'a' && c <= 'z') sb.append((char) ('a' + (c - 'a' + 13) % 26));
            else if (c >= 'A' && c <= 'Z') sb.append((char) ('A' + (c - 'A' + 13) % 26));
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String tryHexDecode(String s) {
        String clean = s.replaceAll("\\s", "");
        if (clean.length() < 8 || clean.length() % 2 != 0) return null;
        if (!clean.matches("[0-9a-fA-F]+")) return null;
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < clean.length(); i += 2) {
                int val = Integer.parseInt(clean.substring(i, i + 2), 16);
                if (val < 32 && val != 9 && val != 10 && val != 13) return null;
                sb.append((char) val);
            }
            return sb.toString();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String tryBase64Decode(String s) {
        if (s.length() < 16 || !s.matches("[A-Za-z0-9+/]+=*")) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(s);
            String text = new String(decoded, StandardCharsets.UTF_8);
            long ctrl = text.chars().filter(c -> c < 32 && c != 9 && c != 10 && c != 13).count();
            return ctrl > 2 ? null : text;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /** @return a new builder */
    public static Builder builder() { return new Builder(); }

    /** Builder for {@link JailbreakDetector}. */
    public static final class Builder {
        private String name = "jailbreak-detector";
        private boolean enabled = true;
        private int priority = 10;
        private Mode mode = Mode.PATTERN;
        private Sensitivity sensitivity = Sensitivity.MEDIUM;
        private boolean multilingualEnabled = true;

        // ── Pattern engine ────────────────────────────────────────────────────
        /** Fully custom engine — overrides all default engine construction. */
        private TextPatternEngine engineOverride = null;

        /** Inline custom patterns (auto-id generated). */
        private final List<Map.Entry<String, Pattern>> customPatternEntries = new ArrayList<>();
        private int customPatternCounter = 0;

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

        public Builder name(String name)          { this.name = name; return this; }
        public Builder enabled(boolean enabled)   { this.enabled = enabled; return this; }
        public Builder priority(int priority)     { this.priority = priority; return this; }
        public Builder mode(Mode mode)            { this.mode = mode; return this; }
        public Builder sensitivity(Sensitivity s) { this.sensitivity = s; return this; }

        /**
         * Plugs in a fully custom {@link TextPatternEngine}.  When set, the default
         * {@link RegexPatternEngine} is not constructed; the custom engine is used as-is.
         * The active spec list is still built from sensitivity/override settings — the
         * caller must ensure the engine can handle all spec ids it will be asked to evaluate.
         *
         * @param engine custom engine implementation
         * @return this builder
         */
        public Builder engine(TextPatternEngine engine) {
            this.engineOverride = engine;
            return this;
        }

        /**
         * Adds a custom inline pattern (compiled with {@link Pattern#CASE_INSENSITIVE}).
         * An auto-generated id is assigned; the pattern is appended after the default
         * sensitivity-based patterns.
         */
        public Builder addCustomPattern(String regex) {
            String id = "custom_inline_" + customPatternCounter++;
            customPatternEntries.add(Map.entry(id,
                    Pattern.compile(regex, Pattern.CASE_INSENSITIVE)));
            return this;
        }

        /** Adds a pre-compiled custom pattern with an auto-generated id. */
        public Builder addCustomPattern(Pattern pattern) {
            String id = "custom_inline_" + customPatternCounter++;
            customPatternEntries.add(Map.entry(id, pattern));
            return this;
        }

        /**
         * <strong>Replaces</strong> all default regex patterns with patterns loaded from
         * {@code sectionKey} in the given YAML file.
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
         * Enables or disables multilingual jailbreak detection for ZH/JA/AR/HI/TR/KO.
         */
        public Builder multilingualEnabled(boolean enabled) {
            this.multilingualEnabled = enabled;
            return this;
        }

        public JailbreakDetector build() { return new JailbreakDetector(this); }
    }
}
