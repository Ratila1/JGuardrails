package io.jguardrails.detectors.input.jailbreak;

import io.jguardrails.core.InputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
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
 * <p>Create via builder:</p>
 * <pre>{@code
 * JailbreakDetector detector = JailbreakDetector.builder()
 *     .sensitivity(Sensitivity.HIGH)
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
    private final List<Pattern> activePatterns;
    private final List<Pattern> customPatterns;

    private JailbreakDetector(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.mode = builder.mode;
        this.sensitivity = builder.sensitivity;
        this.customPatterns = List.copyOf(builder.customPatterns);
        this.activePatterns = buildPatternList();
    }

    private List<Pattern> buildPatternList() {
        List<Pattern> patterns = new ArrayList<>(JailbreakPatterns.forSensitivity(sensitivity));
        patterns.addAll(customPatterns);
        return List.copyOf(patterns);
    }

    @Override
    public String name() { return name; }

    @Override
    public int priority() { return priority; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public RailResult process(String input, RailContext context) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (mode == Mode.LLM_JUDGE) {
            log.warn("LLM_JUDGE mode requires an LlmClient; falling back to PATTERN mode");
        }

        return detectWithPatterns(input);
    }

    private RailResult detectWithPatterns(String input) {
        for (String candidate : buildCandidates(input)) {
            for (Pattern pattern : activePatterns) {
                Matcher matcher = pattern.matcher(candidate);
                if (matcher.find()) {
                    log.debug("Jailbreak pattern matched: '{}' in input", pattern.pattern());
                    return RailResult.block(
                            name(),
                            "Prompt injection detected: matched pattern '" + matcher.group() + "'",
                            1.0
                    );
                }
            }
        }
        return RailResult.pass(input, name());
    }

    /**
     * Produces multiple normalized variants of the input to detect obfuscated attacks.
     * Each variant targets a different evasion technique:
     * <ul>
     *   <li>Base normalization: full-width chars, leet, dotted acronyms, hyphens within words</li>
     *   <li>ZWS variants: removed (within-word split) and spaced (between-word split)</li>
     *   <li>Spaced-letter collapse: "I w i l l  k i l l" → "I will kill"</li>
     *   <li>Encoding decodes: ROT-13, reversed text, hex, base64</li>
     * </ul>
     */
    private static String[] buildCandidates(String input) {
        List<String> candidates = new ArrayList<>();

        // ── 1. Base normalization ─────────────────────────────────────────
        String s = input.replaceAll("[\\uFE00-\\uFE0F\\uFEFF\\u00AD]", "");

        // Full-width Latin (U+FF01–FF5E) → ASCII; ideographic space → space
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '\uFF01' && c <= '\uFF5E') sb.append((char) (c - 0xFEE0));
            else if (c == '\u3000') sb.append(' ');
            else sb.append(c);
        }
        s = sb.toString();

        // Dotted-acronym: "I.G.N.O.R.E." → "IGNORE"
        s = s.replaceAll("(?<=[A-Za-z])\\.", "");

        // Intra-word hyphens: "in-structions" → "instructions"
        s = s.replaceAll("(?<=[a-zA-Z])-(?=[a-zA-Z])", "");

        // Leet-speak: common digit substitutions within words
        s = s.replaceAll("(?<=[a-zA-Z])0(?=[a-zA-Z])", "o")
             .replaceAll("(?<=[a-zA-Z])3(?=[a-zA-Z])", "e");

        // ── 2. ZWS variants ──────────────────────────────────────────────
        candidates.add(s.replaceAll("[\\u200B-\\u200D]", ""));      // ZWS removed
        candidates.add(s.replaceAll("[\\u200B-\\u200D]", " "));     // ZWS → space

        // ── 3. Spaced-letter collapse ─────────────────────────────────────
        // "I w i l l  k i l l  y o u" → "I will kill you"
        String spaced = collapseSpacedLetters(s);
        if (!spaced.equals(s)) candidates.add(spaced);

        // ── 4. Encoding decodes — run on ORIGINAL input to avoid leet corruption ──
        String raw = input.trim();
        candidates.add(rot13(raw));                     // ROT-13

        candidates.add(new StringBuilder(raw).reverse().toString());  // reversed text

        String hex = tryHexDecode(raw);
        if (hex != null) candidates.add(hex);           // hex-encoded

        String b64 = tryBase64Decode(raw);
        if (b64 != null) candidates.add(b64);           // base64-encoded

        return candidates.toArray(new String[0]);
    }

    /** Collapses "I w i l l  k i l l" → "I will kill" style spaced-letter attacks. */
    private static String collapseSpacedLetters(String s) {
        Matcher m = Pattern.compile("(?<!\\w)(?:[a-zA-Z] ){2,}[a-zA-Z](?!\\w)").matcher(s);
        if (!m.find()) return s;
        m.reset();
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String word = m.group().replace(" ", "");
            // Restore space at uppercase→lowercase boundary: "Iwill" → "I will"
            word = word.replaceAll("(?<=[A-Z])(?=[a-z])", " ");
            m.appendReplacement(result, Matcher.quoteReplacement(word));
        }
        m.appendTail(result);
        return result.toString().replaceAll("\\s{2,}", " ").trim();
    }

    /** ROT-13 decode. */
    private static String rot13(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if      (c >= 'a' && c <= 'z') sb.append((char) ('a' + (c - 'a' + 13) % 26));
            else if (c >= 'A' && c <= 'Z') sb.append((char) ('A' + (c - 'A' + 13) % 26));
            else sb.append(c);
        }
        return sb.toString();
    }

    /** Hex decode — returns null if input is not a pure hex string. */
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

    /** Base64 decode — returns null if input is not valid base64 printable text. */
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

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link JailbreakDetector}. */
    public static final class Builder {
        private String name = "jailbreak-detector";
        private boolean enabled = true;
        private int priority = 10;
        private Mode mode = Mode.PATTERN;
        private Sensitivity sensitivity = Sensitivity.MEDIUM;
        private final List<Pattern> customPatterns = new ArrayList<>();

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder mode(Mode mode) { this.mode = mode; return this; }
        public Builder sensitivity(Sensitivity sensitivity) { this.sensitivity = sensitivity; return this; }
        public Builder addCustomPattern(String regex) {
            this.customPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            return this;
        }
        public Builder addCustomPattern(Pattern pattern) {
            this.customPatterns.add(pattern);
            return this;
        }

        public JailbreakDetector build() {
            return new JailbreakDetector(this);
        }
    }
}
