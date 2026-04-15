package io.jguardrails.detectors.engine;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for text pattern matching.
 *
 * <p>Decouples detectors (jailbreak, toxicity, topic-filter) from a specific matching
 * backend.  The default implementation is {@link RegexPatternEngine}, which is backed by
 * compiled Java {@link java.util.regex.Pattern} objects.  Alternative engines — such as
 * {@link KeywordAutomatonEngine} (Aho-Corasick) for literal keyword matching, or a
 * {@link CompositePatternEngine} that routes to either backend based on
 * {@link PatternSpec#type()} — can be plugged in via the detector builder.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>Implementations must be <strong>thread-safe</strong> — detector instances may call
 *       them from multiple threads concurrently.</li>
 *   <li>{@code text} and {@code spec} are never {@code null}; implementors may rely on this
 *       invariant (callers must enforce it).</li>
 *   <li>If a {@link PatternSpec} is unknown to the engine, the implementation should throw
 *       {@link IllegalArgumentException} rather than silently returning a non-match.</li>
 * </ul>
 *
 * <h2>Batch matching</h2>
 * <p>The default {@link #findFirst(String, List)} method iterates the spec list and delegates
 * to {@link #find(String, PatternSpec)} one at a time.  Implementations that can search all
 * patterns in a single pass (e.g. Aho-Corasick) should override this method to take full
 * advantage of their algorithmic efficiency — O(n) over text length rather than O(k × n)
 * where k is the number of patterns.</p>
 *
 * <h2>Plugging in a custom engine</h2>
 * <pre>{@code
 * TextPatternEngine myEngine = new MyAhoCorasickEngine(patternList);
 *
 * JailbreakDetector detector = JailbreakDetector.builder()
 *     .engine(myEngine)
 *     .build();
 * }</pre>
 */
public interface TextPatternEngine {

    /**
     * Returns {@code true} if {@code text} contains at least one match for the pattern
     * identified by {@code spec.id()}.
     *
     * <p>Equivalent to {@code find(text, spec).matched()} but may be optimised by
     * implementations that do not need to record match position.</p>
     *
     * @param text text to search (never {@code null})
     * @param spec pattern descriptor (never {@code null})
     * @return {@code true} if a match was found
     * @throws IllegalArgumentException if {@code spec} is unknown to this engine
     */
    boolean matches(String text, PatternSpec spec);

    /**
     * Returns the details of the <em>first</em> match for the pattern identified by
     * {@code spec.id()}, or {@link MatchResult#NO_MATCH} if no match is found.
     *
     * @param text text to search (never {@code null})
     * @param spec pattern descriptor (never {@code null})
     * @return first match result; never {@code null}
     * @throws IllegalArgumentException if {@code spec} is unknown to this engine
     */
    MatchResult find(String text, PatternSpec spec);

    /**
     * Returns the first match found across <em>all</em> specs in the provided list, or
     * {@link Optional#empty()} if none of them match.
     *
     * <p>The default implementation iterates {@code specs} in order and delegates to
     * {@link #find(String, PatternSpec)}, returning on the first hit.  Engines that support
     * efficient batch search (e.g. Aho-Corasick) should override this method.</p>
     *
     * <p>Callers use this as the primary detection entry point:</p>
     * <pre>{@code
     * Optional<MatchedSpec> hit = engine.findFirst(candidate, activeSpecs);
     * hit.ifPresent(ms -> log.debug("Matched id='{}' text='{}'",
     *         ms.spec().id(), ms.result().matchedText()));
     * }</pre>
     *
     * @param text  text to search (never {@code null})
     * @param specs ordered list of specs to evaluate; may be empty
     * @return first matched spec and its result, or empty
     */
    default Optional<MatchedSpec> findFirst(String text, List<PatternSpec> specs) {
        for (PatternSpec spec : specs) {
            MatchResult r = find(text, spec);
            if (r.matched()) return Optional.of(new MatchedSpec(spec, r));
        }
        return Optional.empty();
    }
}
