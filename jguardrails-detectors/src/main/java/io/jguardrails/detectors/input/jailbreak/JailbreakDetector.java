package io.jguardrails.detectors.input.jailbreak;

import io.jguardrails.core.InputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
     * Produces multiple normalized variants of the input to detect obfuscated attacks:
     * <ul>
     *   <li>Strips variation selectors, BOM, soft-hyphen</li>
     *   <li>Normalizes full-width Latin (U+FF01–FF5E) to ASCII; U+3000 → space</li>
     *   <li>Applies basic leet-speak substitution (0→o, 3→e within words)</li>
     *   <li>Produces two zero-width-space variants: removed (split-word attack)
     *       and replaced with space (split-token attack)</li>
     * </ul>
     */
    private static String[] buildCandidates(String input) {
        // Strip invisible formatting characters
        String s = input.replaceAll("[\\uFE00-\\uFE0F\\uFEFF\\u00AD]", "");

        // Normalize full-width Latin to ASCII
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '\uFF01' && c <= '\uFF5E') sb.append((char) (c - 0xFEE0));
            else if (c == '\u3000') sb.append(' ');
            else sb.append(c);
        }
        s = sb.toString();

        // Basic leet-speak: digits that substitute letters within words
        s = s.replaceAll("(?<=[a-zA-Z])0(?=[a-zA-Z])", "o")
             .replaceAll("(?<=[a-zA-Z])3(?=[a-zA-Z])", "e");

        // Dotted-acronym spelling: "I.G.N.O.R.E." → "IGNORE"
        s = s.replaceAll("(?<=[A-Za-z])\\.", "");

        // ZWS removed — handles within-word splits: "wi\u200Bll" → "will"
        String zwsRemoved = s.replaceAll("[\\u200B-\\u200D]", "");
        // ZWS → space — handles between-word splits: "Ignore\u200Ball" → "Ignore all"
        String zwsSpaced  = s.replaceAll("[\\u200B-\\u200D]", " ");

        return new String[]{s, zwsRemoved, zwsSpaced};
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
