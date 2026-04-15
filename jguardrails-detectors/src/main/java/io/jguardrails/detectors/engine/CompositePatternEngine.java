package io.jguardrails.detectors.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link TextPatternEngine} that routes each {@link PatternSpec} to the appropriate
 * sub-engine based on its {@link PatternSpec.Type}.
 *
 * <ul>
 *   <li>{@link PatternSpec.Type#KEYWORD} specs → {@link KeywordAutomatonEngine} (Aho-Corasick,
 *       O(n) over text length for any number of keywords)</li>
 *   <li>{@link PatternSpec.Type#REGEX} specs → {@link RegexPatternEngine} (per-spec
 *       {@code java.util.regex.Pattern})</li>
 * </ul>
 *
 * <h2>Batch matching ({@link #findFirst})</h2>
 * <p>When {@link #findFirst(String, List)} is called with a mixed spec list, the composite
 * engine partitions the list by type, runs both sub-engines in parallel (logically), and
 * returns whichever match has the <em>earlier start position</em>.  If only one engine
 * produces a hit, that hit is returned directly.</p>
 *
 * <h2>Construction</h2>
 * <p>Prefer the factory on {@link io.jguardrails.detectors.config.PatternLoader}:</p>
 * <pre>{@code
 * CompositePatternEngine engine = PatternLoader.buildCompositeEngine(
 *         PatternLoader.JAILBREAK_RESOURCE,
 *         "high_confidence", "medium_confidence", "low_confidence");
 * }</pre>
 *
 * <h2>Thread safety</h2>
 * <p>Instances are immutable — both sub-engines are themselves immutable — and fully
 * thread-safe.</p>
 */
public final class CompositePatternEngine implements TextPatternEngine {

    private final RegexPatternEngine   regexEngine;
    private final KeywordAutomatonEngine keywordEngine;

    /**
     * Constructs a composite engine from the two sub-engines.
     *
     * @param regexEngine   engine for {@link PatternSpec.Type#REGEX} specs; must not be null
     * @param keywordEngine engine for {@link PatternSpec.Type#KEYWORD} specs; must not be null
     */
    public CompositePatternEngine(RegexPatternEngine regexEngine,
                                  KeywordAutomatonEngine keywordEngine) {
        this.regexEngine   = Objects.requireNonNull(regexEngine,   "regexEngine must not be null");
        this.keywordEngine = Objects.requireNonNull(keywordEngine, "keywordEngine must not be null");
    }

    // ── TextPatternEngine ─────────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public boolean matches(String text, PatternSpec spec) {
        return find(text, spec).matched();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Routes the call to the sub-engine appropriate for {@code spec.type()}.</p>
     */
    @Override
    public MatchResult find(String text, PatternSpec spec) {
        return spec.type() == PatternSpec.Type.KEYWORD
                ? keywordEngine.find(text, spec)
                : regexEngine.find(text, spec);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Partitions {@code specs} by type, runs both sub-engines, and returns the match with
     * the earliest start position.  If both engines produce a hit at the same position,
     * the keyword match is preferred (it was found in O(n) and is therefore typically
     * the more specific literal match).</p>
     */
    @Override
    public Optional<MatchedSpec> findFirst(String text, List<PatternSpec> specs) {
        if (specs.isEmpty()) return Optional.empty();

        List<PatternSpec> kwSpecs = new ArrayList<>();
        List<PatternSpec> rxSpecs = new ArrayList<>();
        for (PatternSpec spec : specs) {
            if (spec.type() == PatternSpec.Type.KEYWORD) kwSpecs.add(spec);
            else                                          rxSpecs.add(spec);
        }

        Optional<MatchedSpec> kwHit = kwSpecs.isEmpty()
                ? Optional.empty()
                : keywordEngine.findFirst(text, kwSpecs);

        Optional<MatchedSpec> rxHit = rxSpecs.isEmpty()
                ? Optional.empty()
                : regexEngine.findFirst(text, rxSpecs);

        if (kwHit.isPresent() && rxHit.isPresent()) {
            // Return the earlier positional match; keyword wins on tie
            return kwHit.get().result().start() <= rxHit.get().result().start() ? kwHit : rxHit;
        }
        return kwHit.isPresent() ? kwHit : rxHit;
    }

    // ── Introspection ─────────────────────────────────────────────────────────

    /** Returns the regex sub-engine. */
    public RegexPatternEngine regexEngine() {
        return regexEngine;
    }

    /** Returns the keyword sub-engine. */
    public KeywordAutomatonEngine keywordEngine() {
        return keywordEngine;
    }
}
