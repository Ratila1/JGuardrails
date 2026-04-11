package io.jguardrails.detectors.input.pii;

import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PiiMasker")
class PiiMaskerTest {

    private RailContext context;

    @BeforeEach
    void setUp() {
        context = RailContext.empty();
    }

    @Test
    @DisplayName("email address is masked with REDACT strategy")
    void emailMasked_redactStrategy() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.EMAIL)
                .strategy(PiiMaskingStrategy.REDACT)
                .build();

        RailResult result = masker.process("Email me at john@example.com please", context);

        assertThat(result.isModified()).isTrue();
        assertThat(result.text()).contains("[EMAIL REDACTED]");
        assertThat(result.text()).doesNotContain("john@example.com");
    }

    @Test
    @DisplayName("phone number is masked")
    void phoneMasked() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.PHONE)
                .strategy(PiiMaskingStrategy.REDACT)
                .build();

        RailResult result = masker.process("Call me at +7 999 123-45-67", context);

        assertThat(result.isModified()).isTrue();
        assertThat(result.text()).contains("[PHONE REDACTED]");
    }

    @Test
    @DisplayName("credit card number is masked")
    void creditCardMasked() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.CREDIT_CARD)
                .strategy(PiiMaskingStrategy.REDACT)
                .build();

        RailResult result = masker.process("My card is 4276 1234 5678 9012", context);

        assertThat(result.isModified()).isTrue();
        assertThat(result.text()).contains("[CREDIT_CARD REDACTED]");
        assertThat(result.text()).doesNotContain("4276");
    }

    @Test
    @DisplayName("text without PII passes through unchanged")
    void noPii_passes() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.EMAIL, PiiEntity.PHONE)
                .build();

        RailResult result = masker.process("No PII here, just a regular question.", context);

        assertThat(result.isPassed()).isTrue();
        assertThat(result.text()).isEqualTo("No PII here, just a regular question.");
    }

    @Test
    @DisplayName("MASK_PARTIAL strategy partially hides email")
    void maskPartial_email() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.EMAIL)
                .strategy(PiiMaskingStrategy.MASK_PARTIAL)
                .build();

        RailResult result = masker.process("Contact john@google.com today", context);

        assertThat(result.isModified()).isTrue();
        assertThat(result.text()).doesNotContain("john@google.com");
        assertThat(result.text()).contains("@");
        assertThat(result.text()).contains("***");
    }

    @Test
    @DisplayName("HASH strategy replaces email with hash token")
    void hashStrategy_email() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.EMAIL)
                .strategy(PiiMaskingStrategy.HASH)
                .build();

        RailResult result = masker.process("Email: test@example.com", context);

        assertThat(result.isModified()).isTrue();
        assertThat(result.text()).contains("[EMAIL:");
        assertThat(result.text()).doesNotContain("test@example.com");
    }

    @Test
    @DisplayName("metadata contains list of masked entities")
    void metadata_containsMaskedEntities() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.EMAIL)
                .build();

        RailResult result = masker.process("Reach me at admin@example.com", context);

        assertThat(result.metadata()).containsKey("maskedEntities");
        assertThat(result.metadata().get("maskedCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("masks multiple PII types in a single text")
    void multipleEntities_maskedInSingleText() {
        PiiMasker masker = PiiMasker.builder()
                .entities(PiiEntity.EMAIL, PiiEntity.PHONE)
                .strategy(PiiMaskingStrategy.REDACT)
                .build();

        RailResult result = masker.process(
                "Email: foo@bar.com, Phone: +1 555 000 1234", context);

        assertThat(result.isModified()).isTrue();
        assertThat(result.text()).contains("[EMAIL REDACTED]");
        assertThat(result.text()).contains("[PHONE REDACTED]");
    }
}
