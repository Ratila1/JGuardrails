package io.jguardrails.detectors.engine;

import java.util.Objects;

/**
 * Pairs a matched {@link PatternSpec} with the {@link MatchResult} that triggered it.
 *
 * <p>Returned by {@link TextPatternEngine#findFirst(String, java.util.List)} when any
 * pattern in a given spec list produces a hit.  Carrying the spec alongside the result
 * lets callers log which rule fired and what text was matched without a second lookup.</p>
 *
 * <pre>{@code
 * Optional<MatchedSpec> hit = engine.findFirst(text, activeSpecs);
 * hit.ifPresent(ms -> {
 *     log.debug("Matched: id='{}' category='{}' text='{}'",
 *             ms.spec().id(), ms.spec().category(), ms.result().matchedText());
 *     return RailResult.block(name(), "Pattern '" + ms.result().matchedText() + "' matched");
 * });
 * }</pre>
 *
 * @param spec   the spec that produced the match
 * @param result details of the match (matched text, start/end positions)
 */
public record MatchedSpec(PatternSpec spec, MatchResult result) {

    public MatchedSpec {
        Objects.requireNonNull(spec,   "MatchedSpec.spec must not be null");
        Objects.requireNonNull(result, "MatchedSpec.result must not be null");
    }
}
