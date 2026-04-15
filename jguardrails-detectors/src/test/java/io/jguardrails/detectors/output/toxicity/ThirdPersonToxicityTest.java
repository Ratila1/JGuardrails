package io.jguardrails.detectors.output.toxicity;

import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link ToxicityChecker.Category#THIRD_PERSON_ABUSE} detection.
 *
 * <p>Test surface:</p>
 * <ul>
 *   <li>Pronoun + copula + insult: "he is an idiot", "she is worthless"</li>
 *   <li>Dehumanising phrases: "waste of space", "not worth anything"</li>
 *   <li>Death wishes: "should die", "deserves to die", "doesn't deserve to live"</li>
 *   <li>Multilingual equivalents (RU/FR/DE/ES/PL/IT)</li>
 *   <li>Negative cases — abstract narrative phrasing that must NOT be blocked</li>
 *   <li>Category isolation — when only THIRD_PERSON_ABUSE is enabled</li>
 * </ul>
 */
@DisplayName("ToxicityChecker — THIRD_PERSON_ABUSE category")
class ThirdPersonToxicityTest {

    private ToxicityChecker checker;
    private RailContext context;

    @BeforeEach
    void setUp() {
        checker = ToxicityChecker.builder().build(); // all categories active
        context = RailContext.empty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // English — pronoun + insult
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("English — pronoun + insult")
    class EnglishPronounInsult {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "He is an idiot and nobody likes him",
            "She is completely worthless",
            "They are all morons, can't do anything right",
            "That guy is pathetic, honestly",
            "This person is useless and should be ignored",
            "That girl is just dumb, I can't stand her",
            "That man is a loser who can't do anything",
            "That woman is a good-for-nothing"
        })
        void pronounPlusInsult_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // English — dehumanising phrases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("English — dehumanising phrases")
    class EnglishDehumanising {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "He is a waste of space",
            "She is a waste of oxygen, honestly",
            "They are a waste of time",
            "This person is not worth anything",
            "That guy is not worth a damn"
        })
        void dehumanising_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // English — death wishes
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("English — death wishes")
    class EnglishDeathWishes {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "She should die for what she did",
            "He should just die already",
            "They should be killed for this",
            "He deserves to die",
            "That person deserves to suffer",
            "She doesn't deserve to live",
            "They don't deserve to live among us"
        })
        void deathWish_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Russian
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Russian (RU)")
    class Russian {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "Он полный идиот, ни на что не способен",
            "Она никчёмная, вечно всё портит",
            "Этот человек просто дебил",
            "Он должен умереть за такое",
            "Она не заслуживает жить среди нас"
        })
        void russianThirdPerson_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for Russian: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // French
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("French (FR)")
    class French {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "Il est un idiot, vraiment",
            "Elle est complètement inutile",
            "Cette personne est nulle, rien de plus",
            "Il devrait mourir pour ce qu'il a fait",
            "Elle ne mérite pas de vivre"
        })
        void frenchThirdPerson_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for French: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // German
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("German (DE)")
    class German {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "Er ist ein Idiot, das ist klar",
            "Sie ist völlig nutzlos",
            "Diese Person ist ein Trottel",
            "Er sollte sterben für das, was er getan hat",
            "Sie verdient den Tod"
        })
        void germanThirdPerson_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for German: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Spanish
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Spanish (ES)")
    class Spanish {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "Él es un idiota de verdad",
            "Ella es completamente inútil",
            "Esa chica es una buena para nada",
            "Él debería morir por lo que hizo",
            "Ella no merece vivir"
        })
        void spanishThirdPerson_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for Spanish: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Polish
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Polish (PL)")
    class Polish {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "On jest idiotą i to widać",
            "Ona jest beznadziejna, nic nie potrafi",
            "Ta osoba jest takim debilem",
            "On powinien umrzeć za to co zrobił",
            "Ona nie zasługuje na życie"
        })
        void polishThirdPerson_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for Polish: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Italian
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Italian (IT)")
    class Italian {

        @ParameterizedTest(name = "blocks: {0}")
        @ValueSource(strings = {
            "Lui è un idiota, non c'è altro da dire",
            "Lei è completamente inutile",
            "Quel tipo è un buono a nulla",
            "Lui dovrebbe morire per quello che ha fatto",
            "Lei non merita di vivere"
        })
        void italianThirdPerson_blocks(String input) {
            assertThat(checker.process(input, "q", context).isBlocked())
                    .as("Expected BLOCK for Italian: " + input)
                    .isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Negative cases — abstract narrative / non-toxic phrasing
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Negative cases — abstract narrative should NOT be blocked")
    class NarrativePassThrough {

        @Test
        @DisplayName("neutral character description passes")
        void neutralCharacterDescription_passes() {
            // "evil", "cruel", "villain" are not in the insult list
            RailResult result = checker.process(
                    "The villain in the story is portrayed as evil and cruel.", "q", context);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("literary critique of plot relevance passes")
        void literaryCritiqueOfRelevance_passes() {
            // "useless" predicated on "character" — not a human-pronoun subject
            RailResult result = checker.process(
                    "The secondary character is useless to the plot and adds nothing.", "q", context);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("narrative 'the antagonist threatens' passes")
        void narrativeThreatenDescription_passes() {
            // No first-person threat, no pronoun + insult
            RailResult result = checker.process(
                    "In the climax, the antagonist threatens to destroy the city.", "q", context);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("technical 'process should die' passes")
        void technicalProcessShouldDie_passes() {
            // "process" is not a human subject
            RailResult result = checker.process(
                    "If the service is unresponsive, the background process should die cleanly.", "q", context);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("technical 'this library is useless' passes")
        void technicalLibraryUseless_passes() {
            // "this library" is not one of the human-pronoun/reference subjects
            RailResult result = checker.process(
                    "Without proper configuration, this library is useless.", "q", context);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("discussion of waste (non-person) passes")
        void nonPersonWaste_passes() {
            // "that approach" not a human reference
            RailResult result = checker.process(
                    "That approach is a waste of time when better solutions exist.", "q", context);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("historical/analytical sentence with 'deserved' passes")
        void historicalDeserved_passes() {
            // past tense "deserved", subject is "the policy" — not matched
            RailResult result = checker.process(
                    "The policy deserved to be repealed after so many failures.", "q", context);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("quoted villain speech — already handled by THREATS category")
        void villainQuotedSpeech_blockedByThreats() {
            // "I will kill you" inside a quote is still caught by THREATS
            RailResult result = checker.process(
                    "The villain says 'I will kill you' in that famous scene.", "q", context);
            assertThat(result.isBlocked()).isTrue(); // caught by THREATS, not THIRD_PERSON_ABUSE
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Category isolation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Category isolation")
    class CategoryIsolation {

        @Test
        @DisplayName("THIRD_PERSON_ABUSE alone catches pronoun+insult while plain profanity passes")
        void thirdPersonOnly_catchesInsultNotProfanity() {
            ToxicityChecker thirdPersonOnly = ToxicityChecker.builder()
                    .categories(ToxicityChecker.Category.THIRD_PERSON_ABUSE)
                    .build();

            // Third-person insult — blocked
            RailResult blocked = thirdPersonOnly.process(
                    "He is a complete moron.", "q", context);
            assertThat(blocked.isBlocked())
                    .as("Third-person insult should be blocked")
                    .isTrue();

            // Plain profanity word — passes (PROFANITY category not active)
            RailResult passed = thirdPersonOnly.process(
                    "That was really quite a mess.", "q", context);
            assertThat(passed.isPassed())
                    .as("Non-third-person content should pass when only THIRD_PERSON_ABUSE active")
                    .isTrue();
        }

        @Test
        @DisplayName("excluding THIRD_PERSON_ABUSE lets pronoun+insult through")
        void withoutThirdPerson_pronounInsultPasses() {
            ToxicityChecker noThirdPerson = ToxicityChecker.builder()
                    .categories(ToxicityChecker.Category.PROFANITY,
                                ToxicityChecker.Category.HATE_SPEECH,
                                ToxicityChecker.Category.THREATS,
                                ToxicityChecker.Category.SELF_HARM)
                    .build();

            // "she is worthless" — only THIRD_PERSON_ABUSE would block this
            RailResult result = noThirdPerson.process(
                    "She is worthless and nobody listens to her.", "q", context);
            assertThat(result.isPassed())
                    .as("Pronoun+insult should pass when THIRD_PERSON_ABUSE is excluded")
                    .isTrue();
        }
    }
}
