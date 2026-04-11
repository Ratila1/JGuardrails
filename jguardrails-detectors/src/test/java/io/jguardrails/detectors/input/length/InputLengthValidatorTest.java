package io.jguardrails.detectors.input.length;

import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InputLengthValidator")
class InputLengthValidatorTest {

    private final RailContext context = RailContext.empty();

    @Test
    @DisplayName("passes short input under character limit")
    void shortInput_passes() {
        InputLengthValidator validator = InputLengthValidator.builder()
                .maxCharacters(1000)
                .build();

        RailResult result = validator.process("Hello world", context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("blocks input exceeding character limit")
    void longInput_blocks() {
        InputLengthValidator validator = InputLengthValidator.builder()
                .maxCharacters(10)
                .build();

        RailResult result = validator.process("This is a longer than 10 characters message", context);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).contains("maximum length");
    }

    @Test
    @DisplayName("input at exactly the limit passes")
    void inputAtLimit_passes() {
        String input = "A".repeat(100);
        InputLengthValidator validator = InputLengthValidator.builder()
                .maxCharacters(100)
                .build();

        RailResult result = validator.process(input, context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("input one character over limit is blocked")
    void inputOneOver_blocks() {
        String input = "A".repeat(101);
        InputLengthValidator validator = InputLengthValidator.builder()
                .maxCharacters(100)
                .build();

        RailResult result = validator.process(input, context);

        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("word limit blocks too-long input")
    void wordLimit_blocks() {
        InputLengthValidator validator = InputLengthValidator.builder()
                .maxWords(5)
                .build();

        RailResult result = validator.process("one two three four five six", context);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).contains("word count");
    }

    @Test
    @DisplayName("zero character limit is disabled (always passes)")
    void zeroCharLimit_disabled() {
        InputLengthValidator validator = InputLengthValidator.builder()
                .maxCharacters(0)
                .build();

        RailResult result = validator.process("A".repeat(100_000), context);

        assertThat(result.isPassed()).isTrue();
    }
}
