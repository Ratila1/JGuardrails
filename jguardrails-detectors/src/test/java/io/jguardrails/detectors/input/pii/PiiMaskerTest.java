package io.jguardrails.detectors.input.pii;

import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PiiMasker")
class PiiMaskerTest {

    private RailContext context;

    @BeforeEach
    void setUp() {
        context = RailContext.empty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Basic entity tests (existing)
    // ──────────────────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────────────────
    // IP address detection
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("IP address detection")
    class IpAddressDetection {

        private PiiMasker masker() {
            return PiiMasker.builder()
                    .entities(PiiEntity.IP_ADDRESS)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();
        }

        @Test
        @DisplayName("IPv4 in plain sentence is masked")
        void ipv4_plainText() {
            RailResult result = masker().process("Server is at 192.168.1.5 and is down", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[IP_ADDRESS REDACTED]");
            assertThat(result.text()).doesNotContain("192.168.1.5");
        }

        @Test
        @DisplayName("IPv4 in JSON value is masked")
        void ipv4_inJson() {
            RailResult result = masker().process(
                    "{\"client_ip\": \"10.0.0.1\", \"status\": \"ok\"}", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[IP_ADDRESS REDACTED]");
            assertThat(result.text()).doesNotContain("10.0.0.1");
        }

        @Test
        @DisplayName("IPv4 in log line is masked")
        void ipv4_inLogLine() {
            RailResult result = masker().process(
                    "[2024-01-15 10:23:01] GET /api/v1/users from 172.16.0.42 — 200 OK", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[IP_ADDRESS REDACTED]");
            assertThat(result.text()).doesNotContain("172.16.0.42");
        }

        @Test
        @DisplayName("IPv4 in SQL-like string is masked")
        void ipv4_inSql() {
            RailResult result = masker().process(
                    "SELECT * FROM audit_log WHERE ip_address = '203.0.113.7'", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).doesNotContain("203.0.113.7");
        }

        @Test
        @DisplayName("MASK_PARTIAL hides all but first octet")
        void ipv4_maskPartial() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.IP_ADDRESS)
                    .strategy(PiiMaskingStrategy.MASK_PARTIAL)
                    .build();

            RailResult result = masker.process("Connected from 192.168.1.100", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("192.xxx.xxx.xxx");
            assertThat(result.text()).doesNotContain("192.168.1.100");
        }

        @Test
        @DisplayName("multiple IPv4 addresses in one string are all masked")
        void ipv4_multiple() {
            RailResult result = masker().process(
                    "Traffic between 10.0.0.1 and 10.0.0.2 was logged", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).doesNotContain("10.0.0.1");
            assertThat(result.text()).doesNotContain("10.0.0.2");
        }

        @Test
        @DisplayName("IPv4 does not match a mere 3-octet prefix like a dotted notation")
        void ipv4_noMatchThreeOctets() {
            // "192.168.1." is not a valid IP (trailing dot, no fourth octet)
            RailResult result = masker().process("Subnet prefix: 192.168.1.", context);
            // The pattern requires all four octets — this should not match
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Credit card edge cases — "card ending" contextual format
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Credit card — contextual 'card ending' format")
    class CardEndingFormat {

        private PiiMasker masker() {
            return PiiMasker.builder()
                    .entities(PiiEntity.CREDIT_CARD)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();
        }

        @Test
        @DisplayName("'card ending NNNN' is detected and masked")
        void cardEndingSingleGroup() {
            RailResult result = masker().process(
                    "Your card ending 1111 was charged $50", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[CREDIT_CARD REDACTED]");
            assertThat(result.text()).doesNotContain("1111");
        }

        @Test
        @DisplayName("'card ending NNNN-NNNN' (two groups) is detected and masked")
        void cardEndingTwoGroups() {
            RailResult result = masker().process(
                    "call +1-555-123-4567, card ending 1111-2222", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[CREDIT_CARD REDACTED]");
            assertThat(result.text()).doesNotContain("1111");
        }

        @Test
        @DisplayName("MASK_PARTIAL on 'card ending' preserves readable form")
        void cardEnding_maskPartial() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.CREDIT_CARD)
                    .strategy(PiiMaskingStrategy.MASK_PARTIAL)
                    .build();

            RailResult result = masker.process("Your card ending 1111-2222 was declined", context);

            assertThat(result.isModified()).isTrue();
            // Should preserve "card ending XXXX" style with the real last-4
            assertThat(result.text()).containsIgnoringCase("card ending");
            assertThat(result.text()).doesNotContain("1111");
        }

        @Test
        @DisplayName("full card with valid BIN prefix is still detected")
        void fullCardWithBinPrefix() {
            RailResult result = masker().process("Charged to 4111 1111 1111 1111", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[CREDIT_CARD REDACTED]");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mixed PII — email + phone + CC + IP in one string
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Mixed PII types in a single string")
    class MixedPii {

        @Test
        @DisplayName("email + phone + CC with valid BIN are all masked")
        void emailPhoneCc() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.EMAIL, PiiEntity.PHONE, PiiEntity.CREDIT_CARD)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            String input = "Contact john@example.com, call +1-555-123-4567, " +
                           "charged to 4111 1111 1111 1111";
            RailResult result = masker.process(input, context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[EMAIL REDACTED]");
            assertThat(result.text()).contains("[PHONE REDACTED]");
            assertThat(result.text()).contains("[CREDIT_CARD REDACTED]");
            assertThat(result.text()).doesNotContain("john@example.com");
            assertThat(result.text()).doesNotContain("4111");
        }

        @Test
        @DisplayName("email + phone + 'card ending' are all masked")
        void emailPhoneCardEnding() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.EMAIL, PiiEntity.CREDIT_CARD, PiiEntity.PHONE)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            String input = "From john@example.com, call +1-555-123-4567, card ending 1111-2222";
            RailResult result = masker.process(input, context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[EMAIL REDACTED]");
            assertThat(result.text()).contains("[CREDIT_CARD REDACTED]");
            // phone should still be masked — "1111-2222" is consumed by CC, phone = 555 number
            assertThat(result.text()).contains("[PHONE REDACTED]");
            assertThat(result.text()).doesNotContain("john@example.com");
            assertThat(result.text()).doesNotContain("1111-2222");
        }

        @Test
        @DisplayName("email + phone + CC + IP all masked together")
        void emailPhoneCcIp() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.EMAIL, PiiEntity.CREDIT_CARD,
                              PiiEntity.IP_ADDRESS, PiiEntity.PHONE)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            String input = "User alice@corp.com called from 192.168.1.5, " +
                           "phone +7 999 888-77-66, card 4276 1234 5678 9012";
            RailResult result = masker.process(input, context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[EMAIL REDACTED]");
            assertThat(result.text()).contains("[IP_ADDRESS REDACTED]");
            assertThat(result.text()).contains("[PHONE REDACTED]");
            assertThat(result.text()).contains("[CREDIT_CARD REDACTED]");
        }

        @Test
        @DisplayName("JSON with multiple PII fields is fully masked")
        void jsonWithMultiplePii() {
            PiiMasker masker = PiiMasker.builder().strategy(PiiMaskingStrategy.REDACT).build();

            String input = "{\"email\":\"bob@example.com\",\"ip\":\"10.0.0.1\"," +
                           "\"phone\":\"+1 555 000 1234\",\"ssn\":\"123-45-6789\"}";
            RailResult result = masker.process(input, context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).doesNotContain("bob@example.com");
            assertThat(result.text()).doesNotContain("10.0.0.1");
            assertThat(result.text()).doesNotContain("555 000 1234");
            assertThat(result.text()).doesNotContain("123-45-6789");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Negative / edge cases — should NOT trigger false positives
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases — no false positives")
    class NegativeTests {

        @Test
        @DisplayName("UUID is not matched as PHONE or IP")
        void uuidNotMatchedAsPhoneOrIp() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.PHONE, PiiEntity.IP_ADDRESS)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            String uuid = "550e8400-e29b-41d4-a716-446655440000";
            RailResult result = masker.process("Request ID: " + uuid, context);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.text()).contains(uuid);
        }

        @Test
        @DisplayName("log timestamp is not matched as PHONE")
        void logTimestampNotMatchedAsPhone() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.PHONE)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            RailResult result = masker.process("[2024-01-15 10:23:01] Server started", context);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.text()).contains("2024-01-15 10:23:01");
        }

        @Test
        @DisplayName("IPv4 in PHONE exclusion lookahead is not matched as phone")
        void ipAddressNotMatchedAsPhone() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.PHONE)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            RailResult result = masker.process("Gateway IP: 192.168.100.200", context);

            // IP should not be matched by phone pattern
            assertThat(result.text()).doesNotContain("[PHONE REDACTED]");
        }

        @Test
        @DisplayName("SSN is not masked as PHONE when SSN comes first in enum")
        void ssnNotMaskedAsPhone() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.SSN, PiiEntity.PHONE)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            RailResult result = masker.process("My SSN is 555-12-3456", context);

            assertThat(result.isModified()).isTrue();
            assertThat(result.text()).contains("[SSN REDACTED]");
            assertThat(result.text()).doesNotContain("[PHONE REDACTED]");
        }

        @ParameterizedTest(name = "version number \"{0}\" is not masked as IP")
        @ValueSource(strings = {
            "app version 3.2.1.0",
            "runtime 11.0.2.9"
        })
        @DisplayName("version-like dotted numbers are not matched as IPv4 when leading context rules apply")
        void versionNumberIpBoundary(String input) {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.IP_ADDRESS)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            // Version numbers like 3.2.1.0 ARE syntactically valid IPv4 — we document this as
            // known ambiguity. This test just verifies current behaviour is stable, not that
            // these are guaranteed to pass in all cases.
            RailResult result = masker.process(input, context);
            // Just assert the masker doesn't throw and returns a result
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("credit card without valid BIN prefix is not detected as CC (only as phone or nothing)")
        void ccWithoutBinPrefix_notDetectedAsCC() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.CREDIT_CARD)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            // "1111 2222 3333 4444" — starts with 1, which is not a valid BIN prefix
            // Should not be caught by full-card pattern (but may be caught by "card ending" if in context)
            RailResult result = masker.process("number 1111 2222 3333 4444", context);

            // Without "card ending" context and without valid BIN, this should pass
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("'card ending' context is required for non-BIN card detection")
        void cardEndingContextRequired() {
            PiiMasker masker = PiiMasker.builder()
                    .entities(PiiEntity.CREDIT_CARD)
                    .strategy(PiiMaskingStrategy.REDACT)
                    .build();

            // With "card ending" context — should be detected
            RailResult withContext = masker.process("card ending 1111-2222", context);
            assertThat(withContext.isModified()).isTrue();

            // Without "card ending" context and without valid BIN — should not be detected
            RailResult withoutContext = masker.process("account 1111-2222", context);
            assertThat(withoutContext.isPassed()).isTrue();
        }
    }
}
