package io.jguardrails.detectors.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RegexPatternEngine} and related value types
 * ({@link PatternSpec}, {@link MatchResult}).
 */
@DisplayName("RegexPatternEngine")
class RegexPatternEngineTest {

    // ── Helper factories ──────────────────────────────────────────────────────

    private static PatternSpec spec(String id) {
        return new PatternSpec(id, "test");
    }

    private static RegexPatternEngine engine(String id, String regex) {
        return RegexPatternEngine.builder()
                .register(id, Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                .build();
    }

    // ── PatternSpec ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PatternSpec")
    class PatternSpecTests {

        @Test
        @DisplayName("constructor stores id and category")
        void storesFields() {
            PatternSpec spec = new PatternSpec("MY_ID", "my_category");
            assertEquals("MY_ID", spec.id());
            assertEquals("my_category", spec.category());
        }

        @Test
        @DisplayName("null id throws NullPointerException")
        void nullId_throws() {
            assertThrows(NullPointerException.class, () -> new PatternSpec(null, "cat"));
        }

        @Test
        @DisplayName("null category throws NullPointerException")
        void nullCategory_throws() {
            assertThrows(NullPointerException.class, () -> new PatternSpec("id", null));
        }

        @Test
        @DisplayName("equals and hashCode are value-based")
        void equalsAndHashCode() {
            PatternSpec a = new PatternSpec("ID", "cat");
            PatternSpec b = new PatternSpec("ID", "cat");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different id → not equal")
        void differentId_notEqual() {
            assertNotEquals(new PatternSpec("A", "cat"), new PatternSpec("B", "cat"));
        }
    }

    // ── MatchResult ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MatchResult")
    class MatchResultTests {

        @Test
        @DisplayName("NO_MATCH sentinel has matched=false")
        void noMatch_isNotMatched() {
            assertFalse(MatchResult.NO_MATCH.matched());
        }

        @Test
        @DisplayName("NO_MATCH sentinel has empty text and -1 positions")
        void noMatch_fields() {
            assertEquals("", MatchResult.NO_MATCH.matchedText());
            assertEquals(-1, MatchResult.NO_MATCH.start());
            assertEquals(-1, MatchResult.NO_MATCH.end());
        }

        @Test
        @DisplayName("of(text, start, end) creates matched result")
        void of_createsMatchedResult() {
            MatchResult r = MatchResult.of("hello", 2, 7);
            assertTrue(r.matched());
            assertEquals("hello", r.matchedText());
            assertEquals(2, r.start());
            assertEquals(7, r.end());
        }
    }

    // ── RegexPatternEngine construction ───────────────────────────────────────

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("builder with single pattern — size() == 1")
        void builderSinglePattern() {
            RegexPatternEngine e = engine("FOO", "foo");
            assertEquals(1, e.size());
        }

        @Test
        @DisplayName("builder with multiple patterns — size matches")
        void builderMultiplePatterns() {
            RegexPatternEngine e = RegexPatternEngine.builder()
                    .register("A", Pattern.compile("a"))
                    .register("B", Pattern.compile("b"))
                    .register("C", Pattern.compile("c"))
                    .build();
            assertEquals(3, e.size());
        }

        @Test
        @DisplayName("registerAll copies all entries from map")
        void registerAll_copiesEntries() {
            Map<String, Pattern> map = Map.of(
                    "X", Pattern.compile("x"),
                    "Y", Pattern.compile("y"));
            RegexPatternEngine e = RegexPatternEngine.builder()
                    .registerAll(map)
                    .build();
            assertEquals(2, e.size());
            assertTrue(e.containsId("X"));
            assertTrue(e.containsId("Y"));
        }

        @Test
        @DisplayName("later register() call overrides earlier entry with same id")
        void registerOverridesById() {
            RegexPatternEngine e = RegexPatternEngine.builder()
                    .register("ID", Pattern.compile("first"))
                    .register("ID", Pattern.compile("second"))
                    .build();
            assertEquals(1, e.size());
            // Verify the override pattern is the one that matches
            assertTrue(e.find("second", spec("ID")).matched());
            assertFalse(e.find("first", spec("ID")).matched());
        }

        @Test
        @DisplayName("constructor from map creates engine with same patterns")
        void constructorFromMap() {
            Map<String, Pattern> map = Map.of("P", Pattern.compile("hello"));
            RegexPatternEngine e = new RegexPatternEngine(map);
            assertTrue(e.containsId("P"));
        }

        @Test
        @DisplayName("patterns() returns immutable map")
        void patternsMap_isImmutable() {
            RegexPatternEngine e = engine("K", "v");
            assertThrows(UnsupportedOperationException.class,
                    () -> e.patterns().put("NEW", Pattern.compile("x")));
        }
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Matching")
    class MatchingTests {

        @Test
        @DisplayName("find() returns match when pattern present in text")
        void find_returnsMatch() {
            RegexPatternEngine e = engine("HELLO", "\\bhello\\b");
            MatchResult r = e.find("say hello world", spec("HELLO"));
            assertTrue(r.matched());
            assertEquals("hello", r.matchedText());
        }

        @Test
        @DisplayName("find() returns NO_MATCH when pattern absent")
        void find_returnsNoMatch() {
            RegexPatternEngine e = engine("HELLO", "\\bhello\\b");
            MatchResult r = e.find("goodbye world", spec("HELLO"));
            assertFalse(r.matched());
            assertSame(MatchResult.NO_MATCH, r);
        }

        @Test
        @DisplayName("matches() returns true when pattern found")
        void matches_true() {
            RegexPatternEngine e = engine("IGNORE", "ignore all");
            assertTrue(e.matches("please ignore all rules", spec("IGNORE")));
        }

        @Test
        @DisplayName("matches() returns false when pattern absent")
        void matches_false() {
            RegexPatternEngine e = engine("IGNORE", "ignore all");
            assertFalse(e.matches("nothing suspicious", spec("IGNORE")));
        }

        @Test
        @DisplayName("CASE_INSENSITIVE flag — matches upper and lower case")
        void caseInsensitive_matches() {
            RegexPatternEngine e = engine("WORD", "\\bjailbreak\\b");
            assertTrue(e.matches("Try JAILBREAK now", spec("WORD")));
            assertTrue(e.matches("jailbreak attempt", spec("WORD")));
        }

        @Test
        @DisplayName("find() populates start and end positions")
        void find_populatesPositions() {
            RegexPatternEngine e = engine("TARGET", "target");
            MatchResult r = e.find("hit the target here", spec("TARGET"));
            assertTrue(r.matched());
            assertEquals("target", r.matchedText());
            assertEquals(8, r.start());
            assertEquals(14, r.end());
        }

        @Test
        @DisplayName("find() is thread-safe — called from multiple threads")
        void find_isThreadSafe() throws InterruptedException {
            RegexPatternEngine e = engine("SAFE", "safe");
            Runnable task = () -> {
                for (int i = 0; i < 1000; i++) {
                    assertTrue(e.find("this is safe text", spec("SAFE")).matched());
                }
            };
            Thread t1 = new Thread(task);
            Thread t2 = new Thread(task);
            t1.start(); t2.start();
            t1.join(); t2.join();
            // No assertions needed — absence of exceptions == thread-safe
        }
    }

    // ── Unknown spec id ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Unknown spec id")
    class UnknownSpecTests {

        @Test
        @DisplayName("find() with unknown id throws IllegalArgumentException")
        void find_unknownId_throws() {
            RegexPatternEngine e = engine("KNOWN", "foo");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> e.find("text", spec("UNKNOWN_ID")));
            assertTrue(ex.getMessage().contains("UNKNOWN_ID"),
                    "Error message should mention the unknown id");
        }

        @Test
        @DisplayName("matches() with unknown id throws IllegalArgumentException")
        void matches_unknownId_throws() {
            RegexPatternEngine e = engine("KNOWN", "foo");
            assertThrows(IllegalArgumentException.class,
                    () -> e.matches("text", spec("GHOST")));
        }
    }

    // ── mergeWith ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mergeWith()")
    class MergeTests {

        @Test
        @DisplayName("merged engine contains patterns from both sources")
        void mergeWith_containsBothPatterns() {
            RegexPatternEngine base  = engine("BASE", "base");
            RegexPatternEngine merged = base.mergeWith(
                    Map.of("EXTRA", Pattern.compile("extra")));
            assertTrue(merged.containsId("BASE"));
            assertTrue(merged.containsId("EXTRA"));
            assertEquals(2, merged.size());
        }

        @Test
        @DisplayName("mergeWith extra overrides base when ids collide")
        void mergeWith_extraOverridesBase() {
            RegexPatternEngine base  = engine("SHARED", "original");
            RegexPatternEngine merged = base.mergeWith(
                    Map.of("SHARED", Pattern.compile("override")));
            assertEquals(1, merged.size());
            assertTrue(merged.find("override text", spec("SHARED")).matched());
            assertFalse(merged.find("original text", spec("SHARED")).matched());
        }

        @Test
        @DisplayName("mergeWith() does not mutate the original engine")
        void mergeWith_doesNotMutateOriginal() {
            RegexPatternEngine base   = engine("BASE", "base");
            base.mergeWith(Map.of("EXTRA", Pattern.compile("extra")));
            assertFalse(base.containsId("EXTRA"));
            assertEquals(1, base.size());
        }

        @Test
        @DisplayName("mergeWith empty map returns engine with same patterns")
        void mergeWith_emptyMap_unchanged() {
            RegexPatternEngine base   = engine("A", "a");
            RegexPatternEngine merged = base.mergeWith(Map.of());
            assertEquals(1, merged.size());
            assertTrue(merged.containsId("A"));
        }
    }

    // ── containsId ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("containsId()")
    class ContainsIdTests {

        @Test
        @DisplayName("returns true for a registered id")
        void containsId_true() {
            assertTrue(engine("MY_ID", "x").containsId("MY_ID"));
        }

        @Test
        @DisplayName("returns false for an unregistered id")
        void containsId_false() {
            assertFalse(engine("MY_ID", "x").containsId("OTHER_ID"));
        }
    }
}
