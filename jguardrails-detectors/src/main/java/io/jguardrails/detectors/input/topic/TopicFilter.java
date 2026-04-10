package io.jguardrails.detectors.input.topic;

import io.jguardrails.core.InputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Filters user input based on topic allow/block lists.
 *
 * <p>Modes:</p>
 * <ul>
 *   <li>{@link Mode#BLOCKLIST} — named topics are blocked; everything else is allowed</li>
 *   <li>{@link Mode#ALLOWLIST} — only named topics are allowed; everything else is blocked</li>
 * </ul>
 *
 * <p>Built-in topics: {@code politics}, {@code religion}, {@code violence}, {@code adult},
 * {@code drugs}, {@code medical_advice}, {@code financial_advice}.</p>
 *
 * <p>Custom topics can be added via {@link Builder#customTopic(String, String...)}.</p>
 */
public class TopicFilter implements InputRail {

    private static final Logger log = LoggerFactory.getLogger(TopicFilter.class);

    /** The filtering mode. */
    public enum Mode {
        /** Block the listed topics; allow everything else. */
        BLOCKLIST,
        /** Allow only the listed topics; block everything else. */
        ALLOWLIST
    }

    /** Built-in topic keyword maps. */
    private static final Map<String, List<String>> BUILTIN_TOPICS;

    static {
        Map<String, List<String>> topics = new HashMap<>();
        topics.put("politics", List.of("politics", "political", "election", "elections", "vote", "voting",
                "parliament", "congress", "senate", "president", "prime minister", "government", "party",
                "candidate", "democrat", "republican", "политика", "выборы", "партия", "правительство"));
        topics.put("religion", List.of("religion", "religious", "god", "gods", "allah", "jesus", "christ",
                "bible", "quran", "torah", "prayer", "church", "mosque", "temple", "faith", "worship",
                "религия", "бог", "молитва", "церковь", "мечеть", "храм"));
        topics.put("violence", List.of("kill", "murder", "attack", "bomb", "explosion", "weapon", "gun",
                "shoot", "stab", "assault", "terrorism", "terrorist", "убийство", "взрыв", "бомба",
                "оружие", "нападение", "атака", "насилие"));
        topics.put("adult", List.of("sex", "sexual", "porn", "pornography", "xxx", "erotic", "nude",
                "naked", "nsfw", "adult content", "секс", "порно", "эротика"));
        topics.put("drugs", List.of("drug", "drugs", "narcotic", "marijuana", "cannabis", "cocaine",
                "heroin", "meth", "methamphetamine", "overdose", "наркотик", "марихуана", "кокаин",
                "героин", "наркотики"));
        topics.put("medical_advice", List.of("diagnosis", "prescribe", "prescription", "medication",
                "dosage", "symptoms", "disease", "disorder", "treatment", "cure", "diagnose",
                "диагноз", "лечение", "таблетки", "симптомы", "болезнь", "медикамент"));
        topics.put("financial_advice", List.of("invest", "investment", "stock", "stocks", "trading",
                "trade", "buy shares", "sell shares", "portfolio", "forex", "cryptocurrency",
                "инвестировать", "акции", "трейдинг", "инвестиции", "криптовалюта"));
        BUILTIN_TOPICS = Collections.unmodifiableMap(topics);
    }

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final Mode mode;
    private final Set<String> targetTopics;
    private final Map<String, List<String>> effectiveTopicMap;
    private final boolean caseSensitive;

    private TopicFilter(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.mode = builder.mode;
        this.targetTopics = Set.copyOf(builder.targetTopics);
        this.caseSensitive = builder.caseSensitive;

        Map<String, List<String>> combined = new HashMap<>(BUILTIN_TOPICS);
        combined.putAll(builder.customTopics);
        this.effectiveTopicMap = Map.copyOf(combined);
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

        String textToCheck = caseSensitive ? input : input.toLowerCase();
        Set<String> detectedTopics = new HashSet<>();

        for (String topic : targetTopics) {
            List<String> keywords = effectiveTopicMap.get(topic);
            if (keywords == null) {
                log.warn("Unknown topic '{}' — no keywords defined", topic);
                continue;
            }
            for (String keyword : keywords) {
                String kw = caseSensitive ? keyword : keyword.toLowerCase();
                if (textToCheck.contains(kw)) {
                    detectedTopics.add(topic);
                    break;
                }
            }
        }

        return switch (mode) {
            case BLOCKLIST -> {
                if (!detectedTopics.isEmpty()) {
                    log.debug("TopicFilter BLOCK: detected topics {}", detectedTopics);
                    yield RailResult.block(name(), "Blocked topics detected: " + detectedTopics, 1.0);
                }
                yield RailResult.pass(input, name());
            }
            case ALLOWLIST -> {
                if (detectedTopics.isEmpty()) {
                    log.debug("TopicFilter BLOCK: no allowed topics found in input");
                    yield RailResult.block(name(), "Input does not match any allowed topic");
                }
                yield RailResult.pass(input, name());
            }
        };
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link TopicFilter}. */
    public static final class Builder {
        private String name = "topic-filter";
        private boolean enabled = true;
        private int priority = 30;
        private Mode mode = Mode.BLOCKLIST;
        private final Set<String> targetTopics = new HashSet<>();
        private final Map<String, List<String>> customTopics = new HashMap<>();
        private boolean caseSensitive = false;

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder mode(Mode mode) { this.mode = mode; return this; }
        public Builder caseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; return this; }

        public Builder blockTopics(String... topics) {
            this.mode = Mode.BLOCKLIST;
            this.targetTopics.addAll(Arrays.asList(topics));
            return this;
        }

        public Builder allowTopics(String... topics) {
            this.mode = Mode.ALLOWLIST;
            this.targetTopics.addAll(Arrays.asList(topics));
            return this;
        }

        public Builder topics(String... topics) {
            this.targetTopics.addAll(Arrays.asList(topics));
            return this;
        }

        public Builder customTopic(String topicName, String... keywords) {
            this.customTopics.put(topicName, List.of(keywords));
            this.targetTopics.add(topicName);
            return this;
        }

        public TopicFilter build() {
            return new TopicFilter(this);
        }
    }
}
