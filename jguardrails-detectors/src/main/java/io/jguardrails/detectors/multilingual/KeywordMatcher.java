package io.jguardrails.detectors.multilingual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lightweight keyword-matching engine for multilingual text detection.
 *
 * <p>Unlike regex-based detectors, this engine uses plain {@code String.contains()} which
 * is both fast and correct for scripts where regex word boundaries ({@code \b}) are
 * undefined — specifically CJK (Chinese, Japanese, Korean), Arabic, and Devanagari (Hindi).</p>
 *
 * <h2>Why not regex for these scripts?</h2>
 * <p>Java's {@code \b} word-boundary assertion is defined at the transition between a
 * {@code \w} character and a non-{@code \w} character. Since {@code \w} is {@code [a-zA-Z0-9_]}
 * (ASCII-only by default, even with {@code UNICODE_CHARACTER_CLASS}), CJK, Arabic and Devanagari
 * characters are all treated as non-{@code \w}. This means {@code \b} fires at <em>every</em>
 * character boundary in these scripts, making pattern anchoring unreliable.
 * A substring {@code contains()} check is simpler and more predictable.</p>
 *
 * <h2>Matching semantics</h2>
 * <ul>
 *   <li>All keywords are pre-lowercased at construction time ({@link Locale#ROOT}).</li>
 *   <li>The input text is lowercased at match time before comparison.</li>
 *   <li>This is correct for Latin (EN, FR, DE, ES, TR…), Cyrillic, and Greek scripts, and
 *       is a no-op for CJK, Arabic, and Devanagari (those scripts have no case).</li>
 * </ul>
 */
public final class KeywordMatcher {

    private final List<String> lowercasedKeywords;

    /**
     * Constructs a matcher from the given keyword collection.
     *
     * @param keywords keywords to match; must not be {@code null}
     */
    public KeywordMatcher(Collection<String> keywords) {
        List<String> lc = new ArrayList<>(keywords.size());
        for (String kw : keywords) {
            lc.add(kw.toLowerCase(Locale.ROOT));
        }
        this.lowercasedKeywords = List.copyOf(lc);
    }

    /**
     * Returns {@code true} if the text contains any registered keyword.
     *
     * @param text text to search; must not be {@code null}
     * @return {@code true} if at least one keyword is found
     */
    public boolean matches(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : lowercasedKeywords) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first keyword found in the text, or {@link Optional#empty()} if none.
     *
     * @param text text to search; must not be {@code null}
     * @return optional first matching keyword (in its lowercased form)
     */
    public Optional<String> firstMatch(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : lowercasedKeywords) {
            if (lower.contains(kw)) {
                return Optional.of(kw);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the number of registered keywords.
     *
     * @return keyword count
     */
    public int size() {
        return lowercasedKeywords.size();
    }
}
