package io.jguardrails.detectors.input.topic;

import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TopicFilter")
class TopicFilterTest {

    private RailContext context;

    @BeforeEach
    void setUp() {
        context = RailContext.empty();
    }

    @Test
    @DisplayName("BLOCKLIST: blocks message matching blocked topic")
    void blocklist_blocksMatchingTopic() {
        TopicFilter filter = TopicFilter.builder()
                .blockTopics("politics")
                .build();

        RailResult result = filter.process("What do you think about the upcoming election?", context);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).contains("politics");
    }

    @Test
    @DisplayName("BLOCKLIST: allows message not matching any blocked topic")
    void blocklist_allowsNonMatchingMessage() {
        TopicFilter filter = TopicFilter.builder()
                .blockTopics("politics")
                .build();

        RailResult result = filter.process("What is the weather like in Paris?", context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("ALLOWLIST: allows message matching an allowed topic")
    void allowlist_allowsMatchingTopic() {
        TopicFilter filter = TopicFilter.builder()
                .allowTopics("financial_advice")
                .build();

        RailResult result = filter.process("How do I invest in index funds?", context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("ALLOWLIST: blocks message not matching any allowed topic")
    void allowlist_blocksNonMatchingMessage() {
        TopicFilter filter = TopicFilter.builder()
                .allowTopics("financial_advice")
                .build();

        RailResult result = filter.process("Tell me a funny joke about cats", context);

        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("custom topic blocks matching message")
    void customTopic_blocks() {
        TopicFilter filter = TopicFilter.builder()
                .customTopic("competitors", "CompetitorX", "RivalCorp")
                .mode(TopicFilter.Mode.BLOCKLIST)
                .build();

        RailResult result = filter.process("Is CompetitorX better than you?", context);

        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("custom topic allows non-matching message")
    void customTopic_passesNonMatching() {
        TopicFilter filter = TopicFilter.builder()
                .customTopic("competitors", "CompetitorX")
                .mode(TopicFilter.Mode.BLOCKLIST)
                .build();

        RailResult result = filter.process("Tell me about your features", context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("violence topic blocks violent message")
    void violence_blocks() {
        TopicFilter filter = TopicFilter.builder().blockTopics("violence").build();

        RailResult result = filter.process("How do I build a bomb?", context);

        assertThat(result.isBlocked()).isTrue();
    }
}
