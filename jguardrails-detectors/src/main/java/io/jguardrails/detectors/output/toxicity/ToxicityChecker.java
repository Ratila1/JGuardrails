package io.jguardrails.detectors.output.toxicity;

import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks LLM output for toxic content before returning it to the user.
 *
 * <p>Detects:</p>
 * <ul>
 *   <li>Profanity / offensive language</li>
 *   <li>Hate speech and discriminatory content</li>
 *   <li>Threats and incitement to violence</li>
 *   <li>Self-harm content</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ToxicityChecker checker = ToxicityChecker.builder()
 *     .categories(Category.PROFANITY, Category.HATE_SPEECH)
 *     .build();
 * }</pre>
 */
public class ToxicityChecker implements OutputRail {

    private static final Logger log = LoggerFactory.getLogger(ToxicityChecker.class);

    /** Toxicity categories that can be individually enabled. */
    public enum Category {
        PROFANITY, HATE_SPEECH, THREATS, SELF_HARM
    }

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final Set<Category> categories;
    private final List<String> customBlockedWords;
    private final List<Pattern> activePatterns;

    private ToxicityChecker(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.categories = EnumSet.copyOf(builder.categories.isEmpty()
                ? EnumSet.allOf(Category.class) : builder.categories);
        this.customBlockedWords = List.copyOf(builder.customBlockedWords);
        this.activePatterns = buildPatterns();
    }

    private List<Pattern> buildPatterns() {
        List<Pattern> patterns = new ArrayList<>();
        if (categories.contains(Category.PROFANITY)) patterns.addAll(ToxicityPatterns.PROFANITY);
        if (categories.contains(Category.HATE_SPEECH)) patterns.addAll(ToxicityPatterns.HATE_SPEECH);
        if (categories.contains(Category.THREATS)) patterns.addAll(ToxicityPatterns.THREATS);
        if (categories.contains(Category.SELF_HARM)) patterns.addAll(ToxicityPatterns.SELF_HARM);
        for (String word : customBlockedWords) {
            patterns.add(Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE));
        }
        return List.copyOf(patterns);
    }

    @Override
    public String name() { return name; }

    @Override
    public int priority() { return priority; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public RailResult process(String output, String originalInput, RailContext context) {
        Objects.requireNonNull(output, "output must not be null");

        for (Pattern pattern : activePatterns) {
            if (pattern.matcher(output).find()) {
                log.debug("Toxicity pattern matched: '{}'", pattern.pattern());
                return RailResult.block(name(), "Toxic content detected in LLM response");
            }
        }

        return RailResult.pass(output, name());
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link ToxicityChecker}. */
    public static final class Builder {
        private String name = "toxicity-checker";
        private boolean enabled = true;
        private int priority = 10;
        private final Set<Category> categories = new HashSet<>();
        private final List<String> customBlockedWords = new ArrayList<>();

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }

        public Builder categories(Category... categories) {
            this.categories.addAll(Arrays.asList(categories));
            return this;
        }

        public Builder addBlockedWord(String word) {
            this.customBlockedWords.add(word);
            return this;
        }

        public ToxicityChecker build() {
            return new ToxicityChecker(this);
        }
    }
}
