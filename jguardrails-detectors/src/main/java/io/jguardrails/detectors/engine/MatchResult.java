package io.jguardrails.detectors.engine;

import java.util.Objects;

/**
 * Immutable result of a {@link TextPatternEngine#find} call.
 *
 * <p>Either represents a successful match (when {@link #matched()} is {@code true}) or the
 * absence of a match (use the {@link #NO_MATCH} sentinel).  When matched, {@link #matchedText()},
 * {@link #start()}, and {@link #end()} describe the first occurrence found.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MatchResult result = engine.find(text, spec);
 * if (result.matched()) {
 *     System.out.println("Hit: '" + result.matchedText() + "' at [" + result.start() + ", " + result.end() + ")");
 * }
 * }</pre>
 *
 * @param matched      {@code true} if a match was found
 * @param matchedText  the matched substring (empty string when {@code matched} is {@code false})
 * @param start        start offset of the match, inclusive (−1 when not matched)
 * @param end          end offset of the match, exclusive (−1 when not matched)
 */
public record MatchResult(boolean matched, String matchedText, int start, int end) {

    /** Sentinel value representing the absence of a match. */
    public static final MatchResult NO_MATCH = new MatchResult(false, "", -1, -1);

    public MatchResult {
        Objects.requireNonNull(matchedText, "matchedText must not be null");
    }

    /**
     * Creates a successful match result.
     *
     * @param matchedText the matched substring
     * @param start       start offset (inclusive)
     * @param end         end offset (exclusive)
     * @return a matched {@code MatchResult}
     */
    public static MatchResult of(String matchedText, int start, int end) {
        Objects.requireNonNull(matchedText, "matchedText must not be null");
        return new MatchResult(true, matchedText, start, end);
    }
}
