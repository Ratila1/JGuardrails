package io.jguardrails.detectors.engine;

import java.util.Objects;

/**
 * Metadata descriptor for a single detection pattern.
 *
 * <p>{@code PatternSpec} identifies a pattern by its unique string {@code id}, an optional
 * {@code category} label (e.g. {@code "profanity"}, {@code "high_confidence"}, {@code "pii"}),
 * and a {@link Type} that tells the engine how to match the pattern.</p>
 *
 * <h2>Pattern types</h2>
 * <ul>
 *   <li>{@link Type#REGEX} — matched by a compiled {@link java.util.regex.Pattern}; used for
 *       rules that require alternation, lookaheads, or complex structure.</li>
 *   <li>{@link Type#KEYWORD} — matched by substring search (Aho-Corasick automaton); used for
 *       simple literal phrases where regex overhead is unnecessary.</li>
 * </ul>
 *
 * <p>The YAML format for each section entry is:</p>
 * <pre>{@code
 * high_confidence:
 *   - id: EN_SOME_REGEX
 *     flags: CI
 *     pattern: "\\bsome\\s+pattern\\b"
 *   - id: KW_SIMPLE_PHRASE
 *     type: KEYWORD          # omit or REGEX for regex (default)
 *     pattern: "simple phrase"
 * }</pre>
 *
 * <h2>Design intent</h2>
 * <p>Keeping specs as pure metadata allows detectors to be oblivious to the matching
 * algorithm.  Swapping {@link RegexPatternEngine} for a future Aho-Corasick or ML-backed
 * engine requires no change to detector code — only the engine implementation changes.</p>
 *
 * @param id       unique pattern identifier; must match the {@code id} field declared in the
 *                 YAML config and registered in the active {@link TextPatternEngine}
 * @param category grouping label for this pattern (e.g. the YAML section key such as
 *                 {@code "profanity"} or {@code "high_confidence"})
 * @param type     matching strategy — {@link Type#REGEX} or {@link Type#KEYWORD}
 */
public record PatternSpec(String id, String category, Type type) {

    /** Matching strategy for a {@code PatternSpec}. */
    public enum Type {
        /** Full Java regex, compiled and matched via {@link java.util.regex.Pattern}. */
        REGEX,
        /** Literal substring / keyword, matched via Aho-Corasick automaton. */
        KEYWORD
    }

    public PatternSpec {
        Objects.requireNonNull(id,       "PatternSpec.id must not be null");
        Objects.requireNonNull(category, "PatternSpec.category must not be null");
        Objects.requireNonNull(type,     "PatternSpec.type must not be null");
    }

    /**
     * Backward-compatible constructor that defaults the type to {@link Type#REGEX}.
     *
     * @param id       pattern identifier
     * @param category category / section label
     */
    public PatternSpec(String id, String category) {
        this(id, category, Type.REGEX);
    }

    @Override
    public String toString() {
        return "PatternSpec[id=" + id + ", category=" + category + ", type=" + type + "]";
    }
}
