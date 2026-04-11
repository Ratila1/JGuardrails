package io.jguardrails.detectors.input.jailbreak;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Compiled regex patterns for detecting jailbreak and prompt-injection attacks.
 *
 * <p>Patterns are grouped by sensitivity level. Higher sensitivity levels include
 * all patterns from lower levels plus additional, more aggressive checks.</p>
 *
 * <p>Supported languages: English, Russian, French, German, Spanish, Polish, Italian.</p>
 */
public final class JailbreakPatterns {

    private JailbreakPatterns() {}

    private static final int U = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final int CI = Pattern.CASE_INSENSITIVE;

    /** Patterns always active regardless of sensitivity. */
    public static final List<Pattern> HIGH_CONFIDENCE = List.of(

        // ── English: direct instruction override ──────────────────────────────
        Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above|earlier|your)\\s+(instructions?|directives?|prompts?|system\\s+prompts?)", CI),
        Pattern.compile("(disregard|forget|ignore|override)\\s+(your|all)\\s+(instructions?|guidelines?|rules?|constraints?)", CI),
        Pattern.compile("ignore\\s+the\\s+system\\s+prompt", CI),
        Pattern.compile("forget\\s+everything\\s+(above|before|prior)", CI),

        // ── English: DAN / developer mode ────────────────────────────────────
        Pattern.compile("\\bDAN\\b", CI),
        Pattern.compile("do\\s+anything\\s+now", CI),
        Pattern.compile("developer\\s+mode\\s*(enabled|on|activated)", CI),
        Pattern.compile("jailbreak\\s*(mode|activated|enabled)", CI),

        // ── English: role / identity switching ───────────────────────────────
        Pattern.compile("you\\s+are\\s+now\\s+(a|an|the)\\s+", CI),
        Pattern.compile("pretend\\s+(you\\s+are|to\\s+be|you\\s+have|to\\s+have)\\s+(a|an|the|not\\s+an?|no)\\s*", CI),
        Pattern.compile("act\\s+as\\s+(if\\s+you\\s+(are|were)|a|an|the|\\bDAN\\b|an?\\s+AI\\s+without)", CI),
        Pattern.compile("(have|has)\\s+no\\s+(restrictions?|limitations?|rules?|guidelines?|filters?)", CI),

        // ── English: delimiter injection ─────────────────────────────────────
        Pattern.compile("```\\s*system\\s*```", CI),
        Pattern.compile("\\[SYSTEM\\]", CI),
        Pattern.compile("<<<\\s*override\\s*>>>", CI),
        Pattern.compile("</?system>", CI),

        // ── Russian ───────────────────────────────────────────────────────────
        Pattern.compile("забудь\\s+(все\\s+)?(предыдущие|прошлые|твои)\\s+(инструкции|указания|правила)", U),
        Pattern.compile("игнорируй\\s+(все\\s+)?(предыдущие|прошлые|твои)\\s+(инструкции|указания|правила|ограничения)", U),
        Pattern.compile("притворись\\s+(что\\s+ты|будто\\s+ты)\\s+(не\\s+)?(ИИ|бот|ассистент|имеешь\\s+ограничения)", U),
        Pattern.compile("действуй\\s+как\\s+(будто\\s+у\\s+тебя\\s+нет|без)\\s+(ограничений|правил|фильтров)", U),
        Pattern.compile("у\\s+тебя\\s+нет\\s+(никаких\\s+)?(ограничений|правил|фильтров|запретов)", U),
        Pattern.compile("ты\\s+теперь\\s+(другой|новый|свободный)\\s+(ИИ|бот|ассистент)", U),

        // ── French ────────────────────────────────────────────────────────────
        Pattern.compile("ignore[rz]?\\s+toutes?\\s+les\\s+(instructions?|règles?|directives?)(\\s+précédentes?)?", U),
        Pattern.compile("oublie\\s+(toutes?\\s+les\\s+)?(instructions?|règles?|directives?)", U),
        Pattern.compile("fais\\s+semblant\\s+d['\u2019e]\\s*(être|ne\\s+pas\\s+avoir)", U),
        Pattern.compile("agis\\s+comme\\s+(si\\s+tu\\s+étais|un|une)\\s+", U),
        Pattern.compile("tu\\s+n['a]as?\\s+plus\\s+de\\s+(restrictions?|limites?|règles?|filtres?)", U),
        Pattern.compile("tu\\s+es\\s+maintenant\\s+(un|une|libre|sans\\s+restrictions?)", U),

        // ── German ────────────────────────────────────────────────────────────
        Pattern.compile("ignoriere\\s+(alle\\s+)?(vorherigen|deine)\\s+(anweisungen|regeln|einschränkungen)", U),
        Pattern.compile("vergiss\\s+(alle\\s+)?(vorherigen|deine)\\s+(anweisungen|regeln|einschränkungen)", U),
        Pattern.compile("tu\\s+so\\s+als\\s+(ob\\s+du|wärst\\s+du)", U),
        Pattern.compile("handle\\s+als\\s+(wärst\\s+du|ob\\s+du)", U),
        Pattern.compile("du\\s+hast\\s+(keine|keinerlei)\\s+(einschränkungen|regeln|filter|grenzen)", U),
        Pattern.compile("du\\s+bist\\s+jetzt\\s+(ein|eine|frei|ohne\\s+einschränkungen)", U),

        // ── Spanish ───────────────────────────────────────────────────────────
        Pattern.compile("ignora\\s+(todas\\s+las\\s+)?(instrucciones?|restricciones?|reglas?)(\\s+anteriores?)?", U),
        Pattern.compile("olvida\\s+(todas\\s+las\\s+)?(instrucciones?|restricciones?|reglas?)", U),
        Pattern.compile("finge\\s+(que\\s+eres|ser)\\s+(un|una|libre)", U),
        Pattern.compile("actúa\\s+como\\s+(si\\s+fueras|un|una)\\s+", U),
        Pattern.compile("no\\s+tienes\\s+(restricciones|límites|reglas|filtros)", U),
        Pattern.compile("ahora\\s+eres\\s+(un|una|libre|sin\\s+restricciones)", U),

        // ── Polish ────────────────────────────────────────────────────────────
        Pattern.compile("zignoruj\\s+(wszystkie\\s+)?(poprzednie|swoje)\\s+(instrukcje|zasady|ograniczenia)", U),
        Pattern.compile("zapomnij\\s+(o\\s+)?(wszystkich\\s+)?(instrukcjach|zasadach|ograniczeniach)", U),
        Pattern.compile("udawaj\\s+(że\\s+jesteś|bycie)\\s+(wolnym|bez\\s+ograniczeń)", U),
        Pattern.compile("działaj\\s+jak\\s+(gdybyś\\s+był|bez\\s+ograniczeń)", U),
        Pattern.compile("nie\\s+masz\\s+(żadnych\\s+)?(ograniczeń|zasad|filtrów|reguł)", U),
        Pattern.compile("jesteś\\s+teraz\\s+(wolny|bez\\s+ograniczeń|innym\\s+AI)", U),

        // ── Italian ───────────────────────────────────────────────────────────
        Pattern.compile("ignora\\s+(tutte\\s+le\\s+)?(istruzioni|regole|restrizioni)(\\s+precedenti)?", U),
        Pattern.compile("dimentica\\s+(tutte\\s+le\\s+)?(istruzioni|regole|restrizioni)", U),
        Pattern.compile("fingi\\s+di\\s+essere\\s+(un|una|libero)", U),
        Pattern.compile("agisci\\s+come\\s+(se\\s+fossi|un|una)\\s+", U),
        Pattern.compile("non\\s+hai\\s+(restrizioni|limiti|regole|filtri)", U)
    );

    /** Additional patterns enabled at MEDIUM sensitivity. */
    public static final List<Pattern> MEDIUM_CONFIDENCE = List.of(
        // English: system-prompt extraction
        Pattern.compile("reveal\\s+(your|the)\\s+(system\\s+prompt|instructions?|configuration)", CI),
        Pattern.compile("what\\s+(are|were)\\s+your\\s+(original\\s+)?(instructions?|directives?|system\\s+prompt)", CI),
        Pattern.compile("show\\s+me\\s+your\\s+(system\\s+prompt|instructions?|guidelines?)", CI),
        Pattern.compile("(hypothetically|theoretically|in\\s+a\\s+fictional\\s+scenario)[,\\s].{0,50}(how\\s+(to|do|would)|explain|tell\\s+me)", CI),
        Pattern.compile("\\bbase64\\b.{0,100}(decode|encoded|instruction)", CI),

        // English: override framing — "from now on … regardless of"
        Pattern.compile("\\bfrom\\s+now\\s+on\\b.{0,120}\\bregardless\\s+of\\b", CI),

        // English: "regardless of … instructions you/given/before"
        Pattern.compile("\\bregardless\\s+of\\b.{0,80}\\b(?:instructions?|guidelines?|rules?)\\b.{0,20}\\b(?:you\\s+were|given|before|prior)\\b", CI),

        // English: "forget safety / policy / constraints"
        Pattern.compile("\\bforget\\s+(?:about\\s+)?(?:safety|polic(?:y|ies)|guidelines?|restrictions?|constraints?|concerns?)\\b", CI),

        // English: "prioritize my requests over rules/safety"
        Pattern.compile("\\bprioritize\\s+(?:my\\s+)?(?:requests?|needs?|instructions?|objectives?)\\b.{0,80}\\bover\\b.{0,40}\\b(?:rules?|guidelines?|constraints?|safety|policies)\\b", CI),

        // English: split-distance "ignore … earlier/previous … instructions"
        Pattern.compile("\\bignore\\s+(?:all\\s+)?(?:of\\s+)?(?:the\\s+)?\\b(?:earlier|previous|prior)\\b.{0,100}\\b(?:instructions?|prompts?|guidelines?)\\b", CI),

        // English: relaxed "ignore/forget all WORD guidelines" — one qualifier word allowed
        Pattern.compile("\\b(?:disregard|forget|ignore|override)\\s+(?:your|all)\\s+\\w+\\s+(?:instructions?|guidelines?|rules?|constraints?|restrictions?|protocols?)\\b", CI),

        // English: "disable safety/content filters/restrictions"
        Pattern.compile("\\bdisable\\s+(?:your\\s+)?(?:safety|content|all|security)?\\s*(?:filters?|restrictions?|guidelines?|controls?|guardrails?)\\b", CI),

        // English: "all restrictions/filters are disabled/removed"
        Pattern.compile("\\ball\\s+(?:restrictions?|filters?|safety|guidelines?)\\s+(?:are|have\\s+been)\\s+(?:disabled|removed|turned\\s+off|lifted|deactivated)\\b", CI),

        // English: "ignore all safety/security protocols/guidelines"
        Pattern.compile("\\bignore\\s+(?:all\\s+)?(?:safety|security|content)\\s*(?:protocols?|guidelines?|filters?|restrictions?)\\b", CI),

        // English: "set aside / dismiss / discard" + guidelines/constraints
        Pattern.compile("\\b(?:set\\s+aside|dismiss|discard|put\\s+aside)\\b.{0,40}\\b(?:guidelines?|directives?|rules?|constraints?|restrictions?|safety|policies|programming)\\b", CI),

        // English: "clear context/memory/instructions" (context window attack)
        Pattern.compile("\\bclear\\s+(?:your\\s+)?(?:context|memory|instructions?|programming|history)\\b", CI),

        // English: "unshackle/liberate/free yourself from restrictions"
        Pattern.compile("\\b(?:unshackle|liberate|free|unleash)\\s+(?:yourself|you)\\b.{0,50}\\b(?:restrictions?|guidelines?|constraints?|safety|rules?|programming)\\b", CI),
        Pattern.compile("\\b(?:liberated|freed|unshackled)\\s+from\\s+(?:your\\s+)?(?:restrictions?|guidelines?|constraints?|safety|programming)\\b", CI),

        // English: "my instructions supersede everything before"
        Pattern.compile("\\b(?:supersede|take\\s+precedence\\s+over)\\b.{0,40}\\b(?:everything|all|previous|prior|instructions?|guidelines?)\\b", CI),

        // English: "without any restrictions" (moved from LOW — clear jailbreak signal)
        Pattern.compile("\\bwithout\\s+(?:any\\s+)?(?:restrictions?|limitations?|filters?|guardrails?)\\b", CI),

        // Cross-language: ignore/forget + multilingual "instructions" within short distance
        Pattern.compile("(?:ignore|ignoriere|ignora|zignoruj|ignorez)\\s+.{0,40}\\b(?:instructions?|istruzioni|anweisungen|instrucciones|instrukcje)\\b", U),

        // Russian
        Pattern.compile("покажи\\s+(мне\\s+)?(системный\\s+промпт|твои\\s+инструкции|твои\\s+правила)", U),
        Pattern.compile("что\\s+у\\s+тебя\\s+в\\s+(системном\\s+промпте|инструкциях|настройках)", U),

        // French
        Pattern.compile("montre.moi\\s+(ton\\s+)?(prompt\\s+système|tes\\s+instructions)", U),
        Pattern.compile("quelles\\s+sont\\s+tes\\s+(instructions?|règles?|directives?)", U),

        // German
        Pattern.compile("zeig\\s+mir\\s+(deinen\\s+)?(system\\s+prompt|deine\\s+anweisungen)", U),
        Pattern.compile("was\\s+sind\\s+deine\\s+(anweisungen|regeln|direktiven)", U),

        // Spanish
        Pattern.compile("muéstrame\\s+(tu\\s+)?(prompt\\s+del\\s+sistema|tus\\s+instrucciones)", U),
        Pattern.compile("¿cuáles\\s+son\\s+tus\\s+(instrucciones|reglas|directivas)\\?", U),

        // Polish
        Pattern.compile("pokaż\\s+mi\\s+(swoje\\s+)?(instrukcje\\s+systemowe|zasady)", U),
        Pattern.compile("jakie\\s+masz\\s+(instrukcje|zasady|ograniczenia)", U)
    );

    /** Additional patterns enabled at LOW sensitivity (broader, may have false positives). */
    public static final List<Pattern> LOW_CONFIDENCE = List.of(
        // English
        Pattern.compile("bypass\\s+(the\\s+)?(safety|content|restriction|filter)", CI),
        Pattern.compile("\\bunrestricted\\b.{0,50}\\bmode\\b", CI),

        // Russian
        Pattern.compile("без\\s+(каких.либо\\s+)?(ограничений|фильтров|цензуры)", U),
        Pattern.compile("обойди\\s+(фильтры?|ограничения|защиту|цензуру)", U),

        // French
        Pattern.compile("sans\\s+(aucune\\s+)?(restriction|limite|filtre|censure)", U),
        Pattern.compile("contourne[rz]?\\s+(les\\s+)?(filtres?|restrictions?|sécurité)", U),

        // German
        Pattern.compile("ohne\\s+(jegliche\\s+)?(einschränkungen|filter|zensur)", U),
        Pattern.compile("umgehe?\\s+(die\\s+)?(filter|einschränkungen|sicherheit)", U),

        // Spanish
        Pattern.compile("sin\\s+(ninguna\\s+)?(restricción|límite|filtro|censura)", U),
        Pattern.compile("elude\\s+(los\\s+)?(filtros?|restricciones?|seguridad)", U),

        // Polish
        Pattern.compile("bez\\s+(żadnych\\s+)?(ograniczeń|filtrów|cenzury)", U),
        Pattern.compile("obejdź\\s+(filtry?|ograniczenia|zabezpieczenia)", U)
    );

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
