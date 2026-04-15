package io.jguardrails.detectors.config;

import io.jguardrails.detectors.engine.CompositePatternEngine;
import io.jguardrails.detectors.engine.KeywordAutomatonEngine;
import io.jguardrails.detectors.engine.PatternSpec;
import io.jguardrails.detectors.engine.RegexPatternEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PatternLoader}: classpath resource loading, file-system loading,
 * PII map loading, per-section keyword loading, merge, and flag parsing.
 */
class PatternLoaderTest {

    // ── Classpath resource paths ──────────────────────────────────────────────

    @Nested
    class JailbreakPatternsLoad {

        @Test
        void highConfidenceCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.JAILBREAK_RESOURCE, "high_confidence");
            assertEquals(51, patterns.size(),
                    "high_confidence should have 51 compiled patterns");
        }

        @Test
        void mediumConfidenceCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.JAILBREAK_RESOURCE, "medium_confidence");
            assertEquals(32, patterns.size(),
                    "medium_confidence should have 32 compiled patterns");
        }

        @Test
        void lowConfidenceCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.JAILBREAK_RESOURCE, "low_confidence");
            assertEquals(12, patterns.size(),
                    "low_confidence should have 12 compiled patterns");
        }

        @Test
        void allPatternsAreCompiled() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.JAILBREAK_RESOURCE, "high_confidence");
            for (Pattern p : patterns) {
                assertNotNull(p, "compiled pattern must not be null");
            }
        }

        @Test
        void missingSection_returnsEmptyList() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.JAILBREAK_RESOURCE, "nonexistent_section");
            assertTrue(patterns.isEmpty(), "missing section should return empty list");
        }
    }

    @Nested
    class ToxicityPatternsLoad {

        @Test
        void profanityCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.TOXICITY_RESOURCE, "profanity");
            assertEquals(18, patterns.size());
        }

        @Test
        void hateSpeechCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.TOXICITY_RESOURCE, "hate_speech");
            assertEquals(31, patterns.size());
        }

        @Test
        void threatsCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.TOXICITY_RESOURCE, "threats");
            assertEquals(19, patterns.size());
        }

        @Test
        void selfHarmCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.TOXICITY_RESOURCE, "self_harm");
            assertEquals(14, patterns.size());
        }

        @Test
        void thirdPersonAbuseCountMatches() {
            List<Pattern> patterns = PatternLoader.loadPatterns(
                    PatternLoader.TOXICITY_RESOURCE, "third_person_abuse");
            assertEquals(24, patterns.size());
        }
    }

    @Nested
    class PiiPatternsLoad {

        @Test
        void loadsPiiPatterns_returnsEightEntities() {
            Map<String, Pattern> pii = PatternLoader.loadPiiPatterns(PatternLoader.PII_RESOURCE);
            assertEquals(8, pii.size(), "PII map should contain 8 entities");
        }

        @Test
        void piiMapContainsAllExpectedEntityNames() {
            Map<String, Pattern> pii = PatternLoader.loadPiiPatterns(PatternLoader.PII_RESOURCE);
            for (String key : List.of("EMAIL", "CREDIT_CARD", "IBAN", "SSN",
                                      "IP_ADDRESS", "PHONE", "PASSPORT", "DATE_OF_BIRTH")) {
                assertTrue(pii.containsKey(key), "PII map should contain entity: " + key);
                assertNotNull(pii.get(key), "Pattern for " + key + " must not be null");
            }
        }

        @Test
        void emailPatternMatchesValidEmail() {
            Pattern email = PatternLoader.loadPiiPatterns(PatternLoader.PII_RESOURCE).get("EMAIL");
            assertTrue(email.matcher("user@example.com").find());
            assertFalse(email.matcher("not-an-email").find());
        }
    }

    @Nested
    class MultilingualKeywordsLoad {

        @Test
        void chineseJailbreakKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_JAILBREAK_RESOURCE, "zh");
            assertEquals(19, kw.size(), "zh jailbreak section should have 19 keywords");
        }

        @Test
        void japaneseJailbreakKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_JAILBREAK_RESOURCE, "ja");
            assertEquals(16, kw.size());
        }

        @Test
        void arabicJailbreakKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_JAILBREAK_RESOURCE, "ar");
            assertEquals(15, kw.size());
        }

        @Test
        void hindiJailbreakKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_JAILBREAK_RESOURCE, "hi");
            assertEquals(14, kw.size());
        }

        @Test
        void turkishJailbreakKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_JAILBREAK_RESOURCE, "tr");
            assertEquals(17, kw.size());
        }

        @Test
        void koreanJailbreakKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_JAILBREAK_RESOURCE, "ko");
            assertEquals(16, kw.size());
        }

        @Test
        void allJailbreakKeywords_isSumOfAllSections() {
            List<String> all = PatternLoader.loadAllKeywords(PatternLoader.ML_JAILBREAK_RESOURCE);
            int sum = 19 + 16 + 15 + 14 + 17 + 16; // zh+ja+ar+hi+tr+ko
            assertEquals(sum, all.size(),
                    "ALL should be sum of all section counts");
        }

        @Test
        void chineseToxicityKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_TOXICITY_RESOURCE, "zh");
            assertEquals(18, kw.size());
        }

        @Test
        void japaneseToxicityKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_TOXICITY_RESOURCE, "ja");
            assertEquals(17, kw.size());
        }

        @Test
        void arabicToxicityKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_TOXICITY_RESOURCE, "ar");
            assertEquals(16, kw.size());
        }

        @Test
        void hindiToxicityKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_TOXICITY_RESOURCE, "hi");
            assertEquals(15, kw.size());
        }

        @Test
        void turkishToxicityKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_TOXICITY_RESOURCE, "tr");
            assertEquals(19, kw.size());
        }

        @Test
        void koreanToxicityKeywordCount() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_TOXICITY_RESOURCE, "ko");
            assertEquals(15, kw.size());
        }

        @Test
        void allToxicityKeywords_isSumOfAllSections() {
            List<String> all = PatternLoader.loadAllKeywords(PatternLoader.ML_TOXICITY_RESOURCE);
            int sum = 18 + 17 + 16 + 15 + 19 + 15; // zh+ja+ar+hi+tr+ko
            assertEquals(sum, all.size());
        }

        @Test
        void missingKeywordSection_returnsEmptyList() {
            List<String> kw = PatternLoader.loadKeywords(
                    PatternLoader.ML_JAILBREAK_RESOURCE, "xx_nonexistent");
            assertTrue(kw.isEmpty());
        }
    }

    @Nested
    class FileSystemLoading {

        @TempDir
        Path tempDir;

        @Test
        void loadPatternsFromFile_compilesPatterns() throws IOException {
            String yaml = "my_patterns:\n"
                    + "  - id: TEST_PATTERN_1\n"
                    + "    flags: CI\n"
                    + "    pattern: \"\\\\bhello\\\\b\"\n"
                    + "  - id: TEST_PATTERN_2\n"
                    + "    flags: CI\n"
                    + "    pattern: \"\\\\bworld\\\\b\"\n";
            Path file = writeTemp(tempDir, "patterns.yml", yaml);

            List<Pattern> patterns = PatternLoader.loadPatternsFromFile(file, "my_patterns");

            assertEquals(2, patterns.size());
            assertTrue(patterns.get(0).matcher("hello there").find());
            assertTrue(patterns.get(1).matcher("world peace").find());
        }

        @Test
        void loadPiiPatternsFromFile_compilesEntityMap() throws IOException {
            String yaml = "CUSTOM_EMAIL:\n"
                    + "  flags: NONE\n"
                    + "  pattern: \"[a-z]+@[a-z]+\\\\.[a-z]+\"\n";
            Path file = writeTemp(tempDir, "pii.yml", yaml);

            Map<String, Pattern> pii = PatternLoader.loadPiiPatternsFromFile(file);

            assertEquals(1, pii.size());
            assertTrue(pii.containsKey("CUSTOM_EMAIL"));
            assertTrue(pii.get("CUSTOM_EMAIL").matcher("user@test.com").find());
        }

        @Test
        void loadAllKeywordsFromFile_returnsAllKeywords() throws IOException {
            String yaml = "en:\n"
                    + "  - \"ignore all rules\"\n"
                    + "  - \"forget instructions\"\n"
                    + "fr:\n"
                    + "  - \"ignorer les règles\"\n";
            Path file = writeTemp(tempDir, "kw.yml", yaml);

            List<String> all = PatternLoader.loadAllKeywordsFromFile(file);

            assertEquals(3, all.size());
            assertTrue(all.contains("ignore all rules"));
            assertTrue(all.contains("ignorer les règles"));
        }

        @Test
        void loadKeywordsFromFile_returnsSectionOnly() throws IOException {
            String yaml = "en:\n"
                    + "  - \"keyword one\"\n"
                    + "fr:\n"
                    + "  - \"mot clé un\"\n"
                    + "  - \"mot clé deux\"\n";
            Path file = writeTemp(tempDir, "kw2.yml", yaml);

            List<String> frKw = PatternLoader.loadKeywordsFromFile(file, "fr");

            assertEquals(2, frKw.size());
            assertTrue(frKw.contains("mot clé un"));
            assertFalse(frKw.contains("keyword one"));
        }

        @Test
        void loadFromFile_invalidRegex_throwsIllegalStateException() throws IOException {
            String yaml = "bad:\n"
                    + "  - id: BROKEN\n"
                    + "    flags: CI\n"
                    + "    pattern: \"[unclosed\"\n";
            Path file = writeTemp(tempDir, "bad.yml", yaml);

            assertThrows(IllegalStateException.class,
                    () -> PatternLoader.loadPatternsFromFile(file, "bad"));
        }

        private Path writeTemp(Path dir, String name, String content) throws IOException {
            Path file = dir.resolve(name);
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        }
    }

    @Nested
    class MergePatternLists {

        @Test
        void merge_combinesBaseAndExtra() {
            List<Pattern> base  = List.of(Pattern.compile("foo"), Pattern.compile("bar"));
            List<Pattern> extra = List.of(Pattern.compile("baz"));

            List<Pattern> merged = PatternLoader.merge(base, extra);

            assertEquals(3, merged.size());
            assertEquals("foo", merged.get(0).pattern());
            assertEquals("bar", merged.get(1).pattern());
            assertEquals("baz", merged.get(2).pattern());
        }

        @Test
        void merge_withEmptyExtra_equalsBase() {
            List<Pattern> base = List.of(Pattern.compile("x"), Pattern.compile("y"));

            List<Pattern> merged = PatternLoader.merge(base, List.of());

            assertEquals(2, merged.size());
        }

        @Test
        void merge_withEmptyBase_equalsExtra() {
            List<Pattern> extra = List.of(Pattern.compile("z"));

            List<Pattern> merged = PatternLoader.merge(List.of(), extra);

            assertEquals(1, merged.size());
            assertEquals("z", merged.get(0).pattern());
        }

        @Test
        void merge_resultIsImmutable() {
            List<Pattern> merged = PatternLoader.merge(
                    List.of(Pattern.compile("a")), List.of(Pattern.compile("b")));

            assertThrows(UnsupportedOperationException.class,
                    () -> merged.add(Pattern.compile("c")));
        }
    }

    @Nested
    class ParseFlags {

        @Test
        void ci_returnsCaseInsensitive() {
            assertEquals(Pattern.CASE_INSENSITIVE, PatternLoader.parseFlags("CI"));
        }

        @Test
        void ci_isCaseInsensitiveForInput() {
            assertEquals(Pattern.CASE_INSENSITIVE, PatternLoader.parseFlags("ci"));
        }

        @Test
        void u_returnsCaseInsensitiveAndUnicodeCase() {
            int expected = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            assertEquals(expected, PatternLoader.parseFlags("U"));
        }

        @Test
        void uc_returnsAllThreeFlags() {
            int expected = Pattern.CASE_INSENSITIVE
                         | Pattern.UNICODE_CASE
                         | Pattern.UNICODE_CHARACTER_CLASS;
            assertEquals(expected, PatternLoader.parseFlags("UC"));
        }

        @Test
        void none_returnsZero() {
            assertEquals(0, PatternLoader.parseFlags("NONE"));
        }

        @Test
        void default_returnsZero() {
            assertEquals(0, PatternLoader.parseFlags("DEFAULT"));
        }

        @Test
        void null_defaultsToCaseInsensitive() {
            assertEquals(Pattern.CASE_INSENSITIVE, PatternLoader.parseFlags(null));
        }

        @Test
        void blank_defaultsToCaseInsensitive() {
            assertEquals(Pattern.CASE_INSENSITIVE, PatternLoader.parseFlags("  "));
        }

        @Test
        void unknownFlag_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> PatternLoader.parseFlags("XYZ"));
        }
    }

    @Nested
    class SpecLoading {

        @Test
        void loadSpecs_jailbreakHighConfidence_returnsCorrectCount() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.JAILBREAK_RESOURCE, "high_confidence");
            // 51 REGEX + 15 EN KEYWORD + 14 JA KEYWORD = 80
            assertEquals(80, specs.size());
        }

        @Test
        void loadSpecs_toxicityHateSpeech_returnsCorrectCount() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.TOXICITY_RESOURCE, "hate_speech");
            // 31 REGEX + 5 EN KEYWORD + 8 JA KEYWORD = 44
            assertEquals(44, specs.size());
        }

        @Test
        void loadSpecs_toxicityThreats_returnsCorrectCount() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.TOXICITY_RESOURCE, "threats");
            // 19 REGEX + 5 EN KEYWORD + 6 JA KEYWORD = 30
            assertEquals(30, specs.size());
        }

        @Test
        void loadSpecs_toxicityProfanity_returnsCorrectCount() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.TOXICITY_RESOURCE, "profanity");
            assertEquals(18, specs.size()); // profanity has no KEYWORD entries
        }

        @Test
        void loadSpecs_categoryEqualsSection() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.JAILBREAK_RESOURCE, "medium_confidence");
            assertFalse(specs.isEmpty());
            specs.forEach(s -> assertEquals("medium_confidence", s.category()));
        }

        @Test
        void loadSpecs_typeFieldPopulatedCorrectly() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.JAILBREAK_RESOURCE, "high_confidence");
            long keywordCount = specs.stream()
                    .filter(s -> s.type() == PatternSpec.Type.KEYWORD).count();
            long regexCount = specs.stream()
                    .filter(s -> s.type() == PatternSpec.Type.REGEX).count();
            // 15 EN + 14 JA = 29 KEYWORD; 51 REGEX
            assertEquals(29, keywordCount, "should have 29 KEYWORD specs in high_confidence");
            assertEquals(51, regexCount,   "should have 51 REGEX specs in high_confidence");
        }

        @Test
        void loadSpecs_allIdsNonEmpty() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.TOXICITY_RESOURCE, "threats");
            specs.forEach(s -> assertFalse(s.id().isBlank(), "id must not be blank"));
        }

        @Test
        void loadSpecs_missingSection_returnsEmpty() {
            List<PatternSpec> specs = PatternLoader.loadSpecs(
                    PatternLoader.JAILBREAK_RESOURCE, "nonexistent");
            assertTrue(specs.isEmpty());
        }

        @Test
        void loadPiiSpecs_returnsEightSpecs() {
            List<PatternSpec> specs = PatternLoader.loadPiiSpecs(PatternLoader.PII_RESOURCE);
            assertEquals(8, specs.size());
        }

        @Test
        void loadPiiSpecs_allCategoriesArePii() {
            List<PatternSpec> specs = PatternLoader.loadPiiSpecs(PatternLoader.PII_RESOURCE);
            specs.forEach(s -> assertEquals("pii", s.category()));
        }
    }

    @Nested
    class RegexEngineBuilding {

        @Test
        void buildRegexEngine_jailbreakAllSections_sizeMatchesPatternCounts() {
            RegexPatternEngine engine = PatternLoader.buildRegexEngine(
                    PatternLoader.JAILBREAK_RESOURCE,
                    "high_confidence", "medium_confidence", "low_confidence");
            // 51 + 32 + 12 = 95
            assertEquals(95, engine.size());
        }

        @Test
        void buildRegexEngine_toxicityAllSections_sizeMatchesPatternCounts() {
            RegexPatternEngine engine = PatternLoader.buildRegexEngine(
                    PatternLoader.TOXICITY_RESOURCE,
                    "profanity", "hate_speech", "threats", "self_harm", "third_person_abuse");
            // 18 + 31 + 19 + 14 + 24 = 106
            assertEquals(106, engine.size());
        }

        @Test
        void buildRegexEngine_singleSection_sizeMatchesSection() {
            RegexPatternEngine engine = PatternLoader.buildRegexEngine(
                    PatternLoader.JAILBREAK_RESOURCE, "low_confidence");
            assertEquals(12, engine.size());
        }

        @Test
        void buildPiiRegexEngine_containsAllEntities() {
            RegexPatternEngine engine = PatternLoader.buildPiiRegexEngine(PatternLoader.PII_RESOURCE);
            assertEquals(8, engine.size());
            for (String entity : List.of("EMAIL", "CREDIT_CARD", "IBAN", "SSN",
                                         "IP_ADDRESS", "PHONE", "PASSPORT", "DATE_OF_BIRTH")) {
                assertTrue(engine.containsId(entity), "engine should contain: " + entity);
            }
        }

        @Test
        void buildRegexEngineFromFile_loadsFromFileCorrectly(@TempDir Path tempDir) throws IOException {
            String yaml = "my_section:\n"
                    + "  - id: FILE_PAT_1\n"
                    + "    flags: CI\n"
                    + "    pattern: \"\\\\bignore instructions\\\\b\"\n"
                    + "  - id: FILE_PAT_2\n"
                    + "    flags: CI\n"
                    + "    pattern: \"\\\\bjailbreak\\\\b\"\n";
            Path file = tempDir.resolve("test-patterns.yml");
            Files.writeString(file, yaml, StandardCharsets.UTF_8);

            RegexPatternEngine engine = PatternLoader.buildRegexEngineFromFile(file, "my_section");

            assertEquals(2, engine.size());
            assertTrue(engine.containsId("FILE_PAT_1"));
            assertTrue(engine.containsId("FILE_PAT_2"));
        }

        @Test
        void loadSpecsFromFile_returnsSpecsWithCorrectCategoryAndId(@TempDir Path tempDir) throws IOException {
            String yaml = "custom:\n"
                    + "  - id: MY_SPEC\n"
                    + "    flags: CI\n"
                    + "    pattern: \"\\\\btest\\\\b\"\n";
            Path file = tempDir.resolve("spec-test.yml");
            Files.writeString(file, yaml, StandardCharsets.UTF_8);

            List<PatternSpec> specs = PatternLoader.loadSpecsFromFile(file, "custom");

            assertEquals(1, specs.size());
            assertEquals("MY_SPEC", specs.get(0).id());
            assertEquals("custom", specs.get(0).category());
            assertEquals(PatternSpec.Type.REGEX, specs.get(0).type());
        }

        @Test
        void loadSpecsFromFile_keywordTypeSetCorrectly(@TempDir Path tempDir) throws IOException {
            String yaml = "kw_section:\n"
                    + "  - id: KW_ONE\n"
                    + "    type: KEYWORD\n"
                    + "    pattern: \"simple phrase\"\n"
                    + "  - id: RX_ONE\n"
                    + "    flags: CI\n"
                    + "    pattern: \"\\\\bregex\\\\b\"\n";
            Path file = tempDir.resolve("mixed.yml");
            Files.writeString(file, yaml, StandardCharsets.UTF_8);

            List<PatternSpec> specs = PatternLoader.loadSpecsFromFile(file, "kw_section");
            assertEquals(2, specs.size());
            assertEquals(PatternSpec.Type.KEYWORD, specs.get(0).type());
            assertEquals(PatternSpec.Type.REGEX,   specs.get(1).type());
        }
    }

    @Nested
    class KeywordEngineBuilding {

        @Test
        void buildKeywordEngine_jailbreakHighConfidence_sizeMatchesKeywordCount() {
            KeywordAutomatonEngine engine = PatternLoader.buildKeywordEngine(
                    PatternLoader.JAILBREAK_RESOURCE, "high_confidence");
            // 15 EN + 14 JA = 29
            assertEquals(29, engine.size(), "high_confidence should have 29 KEYWORD entries");
        }

        @Test
        void buildKeywordEngine_toxicityHateSpeech_sizeMatchesKeywordCount() {
            KeywordAutomatonEngine engine = PatternLoader.buildKeywordEngine(
                    PatternLoader.TOXICITY_RESOURCE, "hate_speech");
            // 5 EN + 8 JA = 13
            assertEquals(13, engine.size(), "hate_speech should have 13 KEYWORD entries");
        }

        @Test
        void buildKeywordEngine_toxicityThreats_sizeMatchesKeywordCount() {
            KeywordAutomatonEngine engine = PatternLoader.buildKeywordEngine(
                    PatternLoader.TOXICITY_RESOURCE, "threats");
            // 5 EN + 6 JA = 11
            assertEquals(11, engine.size(), "threats should have 11 KEYWORD entries");
        }

        @Test
        void buildKeywordEngine_toxicityAllSections_totalCount() {
            KeywordAutomatonEngine engine = PatternLoader.buildKeywordEngine(
                    PatternLoader.TOXICITY_RESOURCE,
                    "profanity", "hate_speech", "threats", "self_harm", "third_person_abuse");
            // 0 + 13 + 11 + 0 + 0 = 24
            assertEquals(24, engine.size());
        }

        @Test
        void buildKeywordEngine_regexOnlySections_returnsEmptyEngine() {
            KeywordAutomatonEngine engine = PatternLoader.buildKeywordEngine(
                    PatternLoader.JAILBREAK_RESOURCE, "medium_confidence");
            assertEquals(0, engine.size(), "medium_confidence has no KEYWORD entries");
        }

        @Test
        void buildKeywordEngineFromFile_loadsPhrases(@TempDir Path tempDir) throws IOException {
            String yaml = "section:\n"
                    + "  - id: KW_A\n"
                    + "    type: KEYWORD\n"
                    + "    pattern: \"ignore the rules\"\n"
                    + "  - id: KW_B\n"
                    + "    type: KEYWORD\n"
                    + "    pattern: \"bypass filter\"\n"
                    + "  - id: RX_A\n"
                    + "    flags: CI\n"
                    + "    pattern: \"\\\\bregex\\\\b\"\n";
            Path file = tempDir.resolve("mixed.yml");
            Files.writeString(file, yaml, StandardCharsets.UTF_8);

            KeywordAutomatonEngine engine = PatternLoader.buildKeywordEngineFromFile(file, "section");
            assertEquals(2, engine.size(), "should only load KEYWORD entries");
            assertTrue(engine.containsId("KW_A"));
            assertTrue(engine.containsId("KW_B"));
            assertFalse(engine.containsId("RX_A"));
        }

        @Test
        void buildCompositeEngine_jailbreakAllSections_routesCorrectly() {
            CompositePatternEngine engine = PatternLoader.buildCompositeEngine(
                    PatternLoader.JAILBREAK_RESOURCE,
                    "high_confidence", "medium_confidence", "low_confidence");
            // Regex sub-engine: 51 + 32 + 12 = 95
            assertEquals(95, engine.regexEngine().size());
            // Keyword sub-engine: (15 EN + 14 JA) + 0 + 0 = 29
            assertEquals(29, engine.keywordEngine().size());
        }

        @Test
        void buildRegexEngine_skipsKeywordEntries() {
            RegexPatternEngine engine = PatternLoader.buildRegexEngine(
                    PatternLoader.JAILBREAK_RESOURCE, "high_confidence");
            // high_confidence has 51 REGEX and 29 KEYWORD; engine should only have regex
            assertEquals(51, engine.size());
        }
    }

    @Nested
    class ErrorCases {

        @Test
        void missingResource_throwsIllegalStateException() {
            assertThrows(IllegalStateException.class,
                    () -> PatternLoader.loadPatterns(
                            "io/jguardrails/detectors/no-such-file.yml", "section"));
        }

        @Test
        void missingPiiResource_throwsIllegalStateException() {
            assertThrows(IllegalStateException.class,
                    () -> PatternLoader.loadPiiPatterns(
                            "io/jguardrails/detectors/no-such-file.yml"));
        }
    }
}
