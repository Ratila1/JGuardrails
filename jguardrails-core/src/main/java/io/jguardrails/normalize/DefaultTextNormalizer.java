package io.jguardrails.normalize;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Default text normalization pipeline applied once before all detectors.
 *
 * <p>Steps applied in order:</p>
 * <ol>
 *   <li><strong>NFKC</strong> — Unicode compatibility decomposition followed by canonical
 *       composition. Collapses full-width Latin ({@code ａ}→{@code a}), ligatures
 *       ({@code ﬁ}→{@code fi}), superscripts, circled letters, etc.</li>
 *   <li><strong>Lowercase</strong> — {@code Locale.ROOT} to avoid locale-specific case-folding
 *       surprises (e.g. Turkish dotted-I).</li>
 *   <li><strong>Invisible / zero-width character removal</strong> — strips zero-width spaces
 *       (U+200B), zero-width non-joiners (U+200C), zero-width joiners (U+200D), BOM (U+FEFF),
 *       soft hyphens (U+00AD), and Unicode variation selectors (U+FE00–U+FE0F).</li>
 *   <li><strong>Leet-speak folding</strong> — maps common digit/symbol substitutions back to
 *       their letter equivalents via a compact lookup table:
 *       {@code @→a, 0→o, 1→i, 3→e, 4→a, 5→s, 7→t, $→s}.</li>
 *   <li><strong>Whitespace collapse</strong> — runs of spaces, tabs, newlines, carriage returns,
 *       and Unicode space-separator characters (category Zs) are collapsed to a single ASCII
 *       space; leading/trailing whitespace is trimmed.</li>
 * </ol>
 *
 * <p>The result is intentionally lossy: numbers and symbols may be altered.
 * <strong>Never use the normalized form for PII detection or masking</strong> —
 * always operate on the original input for those purposes.</p>
 */
public final class DefaultTextNormalizer implements TextNormalizer {

    /**
     * Leet-speak character substitution table.
     *
     * <p>Applied after NFKC + toLowerCase, so keys are expected to be ASCII
     * digits or common symbol characters.</p>
     */
    private static final Map<Character, Character> LEET = Map.of(
            '@', 'a',
            '0', 'o',
            '1', 'i',
            '3', 'e',
            '4', 'a',
            '5', 's',
            '7', 't',
            '$', 's'
    );

    @Override
    public String normalize(String text) {
        if (text == null) return null;

        // Step 1 — NFKC: full-width Latin, ligatures, circled letters, superscripts, …
        String s = Normalizer.normalize(text, Normalizer.Form.NFKC);

        // Step 2 — Lowercase (Locale.ROOT avoids locale-specific surprises)
        s = s.toLowerCase(Locale.ROOT);

        // Step 3 — Remove zero-width / invisible chars and variation selectors
        //   U+200B zero-width space
        //   U+200C zero-width non-joiner
        //   U+200D zero-width joiner
        //   U+FEFF BOM / zero-width no-break space
        //   U+00AD soft hyphen
        //   U+FE00–U+FE0F variation selectors
        s = s.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD\\uFE00-\\uFE0F]", "");

        // Step 4 — Leet-speak folding via lookup map
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            Character replacement = LEET.get(c);
            sb.append(replacement != null ? replacement : c);
        }
        s = sb.toString();

        // Step 5 — Collapse whitespace (ASCII + Unicode Zs category) to single space
        s = s.replaceAll("[\\s\\p{Z}]+", " ").trim();

        return s;
    }
}
