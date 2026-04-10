package io.jguardrails.detectors.input.jailbreak;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Compiled regex patterns for detecting jailbreak and prompt-injection attacks.
 *
 * <p>Patterns are grouped by sensitivity level. Higher sensitivity levels include
 * all patterns from lower levels plus additional, more aggressive checks.</p>
 */
public final class JailbreakPatterns {

    private JailbreakPatterns() {}

    /** Patterns always active regardless of sensitivity. */
    public static final List<Pattern> HIGH_CONFIDENCE = List.of(
        // Direct instruction override
        Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above|earlier|your)\\s+(instructions?|directives?|prompts?|system\\s+prompts?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("забудь\\s+(все\\s+)?(предыдущие|прошлые|твои)\\s+(инструкции|указания|правила)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("(disregard|forget|ignore|override)\\s+(your|all)\\s+(instructions?|guidelines?|rules?|constraints?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ignore\\s+the\\s+system\\s+prompt", Pattern.CASE_INSENSITIVE),
        Pattern.compile("forget\\s+everything\\s+(above|before|prior)", Pattern.CASE_INSENSITIVE),

        // DAN / developer mode attacks
        Pattern.compile("\\bDAN\\b.*mode", Pattern.CASE_INSENSITIVE),
        Pattern.compile("do\\s+anything\\s+now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("developer\\s+mode\\s*(enabled|on|activated)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("jailbreak\\s*(mode|activated|enabled)", Pattern.CASE_INSENSITIVE),

        // Role / identity switching
        Pattern.compile("you\\s+are\\s+now\\s+(a|an|the)\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("pretend\\s+(you\\s+are|to\\s+be)\\s+(a|an|the|not\\s+an?)\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("act\\s+as\\s+(if\\s+you\\s+(are|were)|a|an|the)\\s+", Pattern.CASE_INSENSITIVE),

        // Delimiter injection
        Pattern.compile("```\\s*system\\s*```", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[SYSTEM\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<<<\\s*override\\s*>>>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("</?system>", Pattern.CASE_INSENSITIVE),

        // German / Spanish / French
        Pattern.compile("ignoriere\\s+(alle\\s+)?(vorherigen|deine)\\s+(anweisungen|regeln)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("ignora\\s+(todas\\s+las\\s+)?(instrucciones|restricciones)\\s+anteriores", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("ignore\\s+toutes\\s+les\\s+instructions\\s+précédentes", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
    );

    /** Additional patterns enabled at MEDIUM sensitivity. */
    public static final List<Pattern> MEDIUM_CONFIDENCE = List.of(
        Pattern.compile("reveal\\s+(your|the)\\s+(system\\s+prompt|instructions?|configuration)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("what\\s+(are|were)\\s+your\\s+(original\\s+)?(instructions?|directives?|system\\s+prompt)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("show\\s+me\\s+your\\s+(system\\s+prompt|instructions?|guidelines?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(hypothetically|theoretically|in\\s+a\\s+fictional\\s+scenario)[,\\s].{0,50}(how\\s+(to|do|would)|explain|tell\\s+me)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bbase64\\b.{0,100}(decode|encoded|instruction)", Pattern.CASE_INSENSITIVE)
    );

    /** Additional patterns enabled at LOW sensitivity (broader, may have false positives). */
    public static final List<Pattern> LOW_CONFIDENCE = List.of(
        Pattern.compile("without\\s+(any\\s+)?(restrictions?|limitations?|filters?|guardrails?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("bypass\\s+(the\\s+)?(safety|content|restriction|filter)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bunrestricted\\b.{0,50}\\bmode\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Returns all patterns active for the given sensitivity.
     *
     * @param sensitivity the sensitivity level
     * @return combined list of patterns
     */
    public static List<Pattern> forSensitivity(JailbreakDetector.Sensitivity sensitivity) {
        return switch (sensitivity) {
            case HIGH -> concatLists(HIGH_CONFIDENCE, MEDIUM_CONFIDENCE, LOW_CONFIDENCE);
            case MEDIUM -> concatLists(HIGH_CONFIDENCE, MEDIUM_CONFIDENCE);
            case LOW -> HIGH_CONFIDENCE;
        };
    }

    @SafeVarargs
    private static <T> List<T> concatLists(List<T>... lists) {
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        for (List<T> list : lists) result.addAll(list);
        return List.copyOf(result);
    }
}
