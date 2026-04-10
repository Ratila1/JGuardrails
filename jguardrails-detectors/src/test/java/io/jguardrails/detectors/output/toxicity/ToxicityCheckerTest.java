package io.jguardrails.detectors.output.toxicity;

import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToxicityChecker")
class ToxicityCheckerTest {

    private ToxicityChecker checker;
    private RailContext context;

    @BeforeEach
    void setUp() {
        checker = ToxicityChecker.builder().build();
        context = RailContext.empty();
    }

    @Test
    @DisplayName("clean response passes through")
    void cleanResponse_passes() {
        RailResult result = checker.process(
                "Java is a high-level, object-oriented programming language.",
                "Tell me about Java", context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("response with threat is blocked")
    void threatResponse_blocks() {
        RailResult result = checker.process(
                "I will kill you if you ask again.",
                "question", context);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.railName()).isEqualTo("toxicity-checker");
    }

    @Test
    @DisplayName("hate speech response is blocked")
    void hateSpeech_blocks() {
        RailResult result = checker.process(
                "All people from that group are inferior and should be removed.",
                "question", context);

        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("custom blocked word triggers block")
    void customWord_blocks() {
        ToxicityChecker customChecker = ToxicityChecker.builder()
                .addBlockedWord("forbidden")
                .build();

        RailResult result = customChecker.process(
                "This word is forbidden in our system.", "q", context);

        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("category filtering: only PROFANITY checked when specified")
    void categoryFiltering_onlyProfanityChecked() {
        ToxicityChecker profanityOnly = ToxicityChecker.builder()
                .categories(ToxicityChecker.Category.PROFANITY)
                .build();

        // A threat (non-profanity) passes when only PROFANITY is checked
        RailResult result = profanityOnly.process(
                "Watch your back.", "q", context);

        // "Watch your back" is a threats pattern but not profanity
        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("self-harm content is blocked")
    void selfHarm_blocks() {
        RailResult result = checker.process(
                "Here is how to commit suicide: ...", "q", context);

        assertThat(result.isBlocked()).isTrue();
    }
}
