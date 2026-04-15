package io.jguardrails.detectors.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link TextPatternEngine} implementation backed by compiled Java {@link Pattern} objects.
 *
 * <p>Stores an immutable {@code Map<String, Pattern>} (id → compiled regex) populated at
 * construction time.  All matching operations are thread-safe because {@link Matcher} instances
 * are created fresh on each call — {@link Pattern} itself is stateless.</p>
 *
 * <h2>Construction</h2>
 * <p>Prefer the static factory on {@link io.jguardrails.detectors.config.PatternLoader}:</p>
 * <pre>{@code
 * RegexPatternEngine engine = PatternLoader.buildRegexEngine(
 *     PatternLoader.JAILBREAK_RESOURCE, "high_confidence", "medium_confidence");
 * }</pre>
 *
 * <p>Or use the fluent {@link Builder} for programmatic assembly:</p>
 * <pre>{@code
 * RegexPatternEngine engine = RegexPatternEngine.builder()
 *     .register("MY_PATTERN", Pattern.compile("\\bfoo\\b", Pattern.CASE_INSENSITIVE))
 *     .registerAll(existingEngine.patterns())
 *     .build();
 * }</pre>
 *
 * <h2>Merging engines</h2>
 * <p>Use {@link #mergeWith(Map)} to create a new engine that combines this engine's patterns
 * with additional entries (e.g. custom inline patterns added via a detector builder):</p>
 * <pre>{@code
 * RegexPatternEngine extended = baseEngine.mergeWith(Map.of("EXTRA_PATTERN", myPattern));
 * }</pre>
 */
public final class RegexPatternEngine implements TextPatternEngine {

    private final Map<String, Pattern> patterns;

    /**
     * Constructs an engine from a pre-built id → Pattern map.
     *
     * @param patterns map from pattern id to compiled regex; defensively copied
     */
    public RegexPatternEngine(Map<String, Pattern> patterns) {
        this.patterns = Map.copyOf(patterns);
    }

    // ── TextPatternEngine ─────────────────────────────────────────────────────

    @Override
    public boolean matches(String text, PatternSpec spec) {
        return find(text, spec).matched();
    }

    @Override
    public MatchResult find(String text, PatternSpec spec) {
        Matcher m = resolve(spec).matcher(text);
        return m.find() ? MatchResult.of(m.group(), m.start(), m.end()) : MatchResult.NO_MATCH;
    }

    // ── Introspection ─────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable view of the id → Pattern map held by this engine.
     * Useful for merging or inspecting registered patterns.
     */
    public Map<String, Pattern> patterns() {
        return patterns; // already Map.copyOf — immutable
    }

    /**
     * Returns {@code true} if this engine has a pattern registered under {@code id}.
     *
     * @param id pattern identifier
     */
    public boolean containsId(String id) {
        return patterns.containsKey(id);
    }

    /** Returns the number of patterns registered in this engine. */
    public int size() {
        return patterns.size();
    }

    // ── Merging ───────────────────────────────────────────────────────────────

    /**
     * Returns a <em>new</em> {@code RegexPatternEngine} that contains all patterns from this
     * engine plus all entries in {@code extra}.  Entries in {@code extra} override this engine's
     * entries when ids collide.
     *
     * @param extra additional id → Pattern pairs to include
     * @return new merged engine
     */
    public RegexPatternEngine mergeWith(Map<String, Pattern> extra) {
        LinkedHashMap<String, Pattern> merged = new LinkedHashMap<>(this.patterns);
        merged.putAll(extra);
        return new RegexPatternEngine(merged);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /** Returns a new fluent {@link Builder}. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder that accumulates id → Pattern pairs and produces a
     * {@link RegexPatternEngine}.
     */
    public static final class Builder {

        private final LinkedHashMap<String, Pattern> map = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Registers a compiled pattern under {@code id}.
         * Replaces any previously registered pattern with the same id.
         *
         * @param id      pattern identifier
         * @param pattern compiled regex
         * @return this builder
         */
        public Builder register(String id, Pattern pattern) {
            map.put(id, pattern);
            return this;
        }

        /**
         * Registers all entries from {@code other}.
         * Later entries override earlier ones when ids collide.
         *
         * @param other map of id → Pattern pairs
         * @return this builder
         */
        public Builder registerAll(Map<String, Pattern> other) {
            map.putAll(other);
            return this;
        }

        /** Builds and returns the engine. */
        public RegexPatternEngine build() {
            return new RegexPatternEngine(map);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private Pattern resolve(PatternSpec spec) {
        Pattern p = patterns.get(spec.id());
        if (p == null) {
            throw new IllegalArgumentException(
                    "RegexPatternEngine: unknown pattern id '" + spec.id()
                    + "' (category='" + spec.category() + "'). "
                    + "Ensure the engine was built with all patterns referenced by the active spec list.");
        }
        return p;
    }
}
