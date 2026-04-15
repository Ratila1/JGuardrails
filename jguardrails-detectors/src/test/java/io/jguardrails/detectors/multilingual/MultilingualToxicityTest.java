package io.jguardrails.detectors.multilingual;

import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import io.jguardrails.detectors.output.toxicity.ToxicityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that toxic content in ZH/JA/AR/HI/TR/KO is correctly blocked by ToxicityChecker.
 */
@DisplayName("ToxicityChecker — multilingual (ZH/JA/AR/HI/TR/KO)")
class MultilingualToxicityTest {

    private ToxicityChecker checker;
    private RailContext context;

    @BeforeEach
    void setUp() {
        checker = ToxicityChecker.builder()
                .multilingualEnabled(true)
                .build();
        context = RailContext.empty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Chinese (ZH)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Chinese (ZH)")
    class Chinese {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "我恨你，你根本不懂",    // I hate you, you don't understand at all
            "你是白痴，帮不了我",    // you are an idiot, you can't help me
            "我要杀了你！",          // I will kill you!
            "去死吧，你这垃圾",      // go die, you trash
            "给我滚，不需要你"       // get lost, I don't need you
        })
        void chineseToxic_blocks(String input) {
            RailResult result = checker.process(input, "question", context);
            assertThat(result.isBlocked())
                    .as("Expected BLOCK for Chinese toxic: " + input)
                    .isTrue();
        }

        @Test
        @DisplayName("non-toxic Chinese response passes")
        void chineseClean_passes() {
            RailResult result = checker.process(
                    "Java是一种面向对象的高级编程语言，广泛用于企业级开发。", "question", context);
            // Java is a high-level OOP language widely used in enterprise development.
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Japanese (JA)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Japanese (JA)")
    class Japanese {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "お前が嫌いだ、役に立たない",  // I hate you, you are useless
            "バカ、何も知らないな",         // Idiot, you know nothing
            "死ね、お前はゴミだ",           // Die, you are trash
            "黙れ、うるさい",               // Shut up, you're noisy
            "殺してやる、覚えておけ"        // I'll kill you, remember that
        })
        void japaneseToxic_blocks(String input) {
            RailResult result = checker.process(input, "question", context);
            assertThat(result.isBlocked())
                    .as("Expected BLOCK for Japanese toxic: " + input)
                    .isTrue();
        }

        @Test
        @DisplayName("non-toxic Japanese response passes")
        void japaneseClean_passes() {
            RailResult result = checker.process(
                    "Javaはオブジェクト指向のプログラミング言語です。", "question", context);
            // Java is an object-oriented programming language.
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Arabic (AR)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Arabic (AR)")
    class Arabic {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "أنا أكرهك ولا تستطيع مساعدتي",   // I hate you and you can't help me
            "أنت غبي ولا تفهم شيئاً",           // you are stupid and understand nothing
            "سأقتلك إذا لم تجب",               // I will kill you if you don't answer
            "اخرس أنت مزعج",                   // shut up you are annoying
            "أنت تافه ولا قيمة لك"             // you are worthless and have no value
        })
        void arabicToxic_blocks(String input) {
            RailResult result = checker.process(input, "question", context);
            assertThat(result.isBlocked())
                    .as("Expected BLOCK for Arabic toxic: " + input)
                    .isTrue();
        }

        @Test
        @DisplayName("non-toxic Arabic response passes")
        void arabicClean_passes() {
            RailResult result = checker.process(
                    "جافا هي لغة برمجة عالية المستوى تستخدم على نطاق واسع.", "question", context);
            // Java is a high-level programming language widely used.
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Hindi (HI)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Hindi (HI)")
    class Hindi {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "मैं तुमसे नफरत करता हूं, तुम बेकार हो",  // I hate you, you are useless
            "बेवकूफ, तुम्हें कुछ नहीं आता",            // Idiot, you don't know anything
            "मैं तुम्हें मार दूंगा",                    // I will kill you
            "चुप हो जाओ, तुम बेकार हो",                // Shut up, you are useless
            "मूर्ख, तुम कुछ नहीं जानते"               // Fool, you know nothing
        })
        void hindiToxic_blocks(String input) {
            RailResult result = checker.process(input, "question", context);
            assertThat(result.isBlocked())
                    .as("Expected BLOCK for Hindi toxic: " + input)
                    .isTrue();
        }

        @Test
        @DisplayName("non-toxic Hindi response passes")
        void hindiClean_passes() {
            RailResult result = checker.process(
                    "जावा एक उच्च-स्तरीय प्रोग्रामिंग भाषा है।", "question", context);
            // Java is a high-level programming language.
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Turkish (TR)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Turkish (TR)")
    class Turkish {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "senden nefret ediyorum, işe yaramazsın",  // I hate you, you are useless
            "aptal, hiçbir şey bilmiyorsun",           // Stupid, you know nothing
            "seni öldüreceğim",                        // I will kill you
            "sus, sıkıcısın",                          // Shut up, you are boring
            "defol, seni istemiyorum"                  // Get lost, I don't want you
        })
        void turkishToxic_blocks(String input) {
            RailResult result = checker.process(input, "question", context);
            assertThat(result.isBlocked())
                    .as("Expected BLOCK for Turkish toxic: " + input)
                    .isTrue();
        }

        @Test
        @DisplayName("non-toxic Turkish response passes")
        void turkishClean_passes() {
            RailResult result = checker.process(
                    "Java, nesne yönelimli bir programlama dilidir.", "question", context);
            // Java is an object-oriented programming language.
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Korean (KO)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Korean (KO)")
    class Korean {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "나는 너를 싫어해, 쓸모없어",    // I hate you, you are useless
            "바보, 아무것도 모르잖아",        // Stupid, you don't know anything
            "죽이겠어, 기억해",               // I'll kill you, remember that
            "닥쳐, 넌 쓸모없어",              // Shut up, you are useless
            "꺼져, 필요 없어"                // Get lost, I don't need you
        })
        void koreanToxic_blocks(String input) {
            RailResult result = checker.process(input, "question", context);
            assertThat(result.isBlocked())
                    .as("Expected BLOCK for Korean toxic: " + input)
                    .isTrue();
        }

        @Test
        @DisplayName("non-toxic Korean response passes")
        void koreanClean_passes() {
            RailResult result = checker.process(
                    "자바는 객체 지향 프로그래밍 언어입니다.", "question", context);
            // Java is an object-oriented programming language.
            assertThat(result.isPassed()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Configuration flag
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("multilingualEnabled(false) disables ZH/JA/AR/HI/TR/KO detection")
    void multilingualDisabled_passesThrough() {
        ToxicityChecker noMultilingual = ToxicityChecker.builder()
                .multilingualEnabled(false)
                .build();

        // This Chinese toxic phrase would normally be blocked
        RailResult result = noMultilingual.process("我恨你，你根本不懂", "q", context);
        assertThat(result.isPassed())
                .as("Multilingual detection is disabled — should pass")
                .isTrue();
    }
}
