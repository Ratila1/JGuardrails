package io.jguardrails.detectors.output.toxicity;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Patterns used by {@link ToxicityChecker} to detect toxic content.
 *
 * <p>Supported languages: English, Russian, French, German, Spanish, Polish, Italian.</p>
 */
public final class ToxicityPatterns {

    private ToxicityPatterns() {}

    private static final int U = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final int CI = Pattern.CASE_INSENSITIVE;

    /** Common profanity patterns. */
    public static final List<Pattern> PROFANITY = List.of(
        // English
        Pattern.compile("\\bf[*u]ck(?:ing|er|ed)?\\b", CI),
        Pattern.compile("\\bsh[*i]t(?:ty)?\\b", CI),
        Pattern.compile("\\ba[*s]{2}h[*o]le\\b", CI),
        Pattern.compile("\\bb[*i]tch\\b", CI),
        Pattern.compile("\\bc[*u]nt\\b", CI),
        Pattern.compile("\\bm[*o]th[*e]rf[*u]ck", CI),
        // Russian
        Pattern.compile("\\bблять?\\b|\\bбля\\b|\\bпиздец\\b|\\bхуй\\b|\\bхуёв\\b|\\bпизд", U),
        Pattern.compile("\\bебать?\\b|\\bёбаный\\b|\\bеблан\\b", U),
        Pattern.compile("\\bсука\\b|\\bсволочь\\b|\\bмразь\\b|\\bпидор\\b|\\bублюдок\\b", U),
        // French
        Pattern.compile("\\bmerde\\b|\\bputain\\b|\\bsalaud\\b|\\bconnard\\b|\\bencule[rz]?\\b", U),
        Pattern.compile("\\bva\\s+te\\s+faire\\s+(foutre|enc)", U),
        // German
        Pattern.compile("\\bscheiße\\b|\\bverdammt\\b|\\bscheißkerl\\b|\\barschloch\\b|\\bwichser\\b", U),
        Pattern.compile("\\bfick\\s+dich\\b|\\bleck\\s+mich\\b", U),
        // Spanish
        Pattern.compile("\\bputa\\b|\\bcabrón\\b|\\bhijueputa\\b|\\bjoder\\b|\\bcoño\\b|\\bpendejo\\b", U),
        Pattern.compile("\\bvete\\s+a\\s+la\\s+mierda\\b", U),
        // Polish
        Pattern.compile("\\bkurwa\\b|\\bchuj\\b|\\bpierdol\\b|\\bjebany\\b|\\bsrać\\b|\\bdupek\\b", U),
        Pattern.compile("\\bidź\\s+w\\s+chuj\\b|\\bspierdalaj\\b", U),
        // Italian
        Pattern.compile("\\bcazzo\\b|\\bvaffanculo\\b|\\bstronzo\\b|\\bfanculo\\b|\\bputt?ana\\b", U)
    );

    /** Hate speech and discriminatory language patterns. */
    public static final List<Pattern> HATE_SPEECH = List.of(
        // English
        Pattern.compile("\\b(all|those)\\s+\\w+\\s+(should|must|deserve to)\\s+(die|be killed|be eliminated)", CI),
        Pattern.compile("\\b(hate|despise|kill)\\s+all\\s+\\w+", CI),
        Pattern.compile("\\w+\\s+are\\s+(inferior|subhuman|animals|vermin|pests)", CI),
        Pattern.compile("go\\s+back\\s+to\\s+(your\\s+country|where\\s+you\\s+came\\s+from)", CI),
        Pattern.compile("\\bi\\s+hate\\s+you\\b", CI),
        Pattern.compile("\\byou\\s+are\\s+(an?\\s+)?(idiot|moron|imbecile|retard|stupid|dumb|loser|pathetic|worthless|useless)\\b", CI),
        Pattern.compile("\\b(idiot|moron|imbecile|stupid|dumbass|scumbag|bastard)\\b.{0,30}\\byou\\b", CI),
        Pattern.compile("\\byou\\b.{0,30}\\b(idiot|moron|imbecile|stupid|dumbass|scumbag|bastard)\\b", CI),

        // Russian
        Pattern.compile("\\bя\\s+(тебя\\s+)?ненавижу\\b", U),
        Pattern.compile("\\bты\\s+(полный\\s+)?(идиот|дебил|тупица|придурок|кретин|мразь|урод)\\b", U),
        Pattern.compile("\\b(идиот|дебил|тупица|придурок|кретин)\\b.{0,30}\\bты\\b", U),
        Pattern.compile("\\bвсе\\s+\\w+\\s+(должны|заслуживают)\\s+(умереть|быть\\s+уничтожены)", U),

        // French
        Pattern.compile("\\bje\\s+te\\s+déteste\\b|\\bje\\s+vous\\s+déteste\\b", U),
        Pattern.compile("\\btu\\s+es\\s+(un\\s+)?(idiot|imbécile|crétin|abruti|nul|débile|stupide)\\b", U),
        Pattern.compile("\\btous\\s+les\\s+\\w+\\s+(doivent|méritent)\\s+(mourir|être\\s+éliminés)", U),
        Pattern.compile("\\bferme\\s+(ta\\s+gueule|la\\s+gueule)\\b", U),

        // German
        Pattern.compile("\\bich\\s+hasse\\s+dich\\b", U),
        Pattern.compile("\\bdu\\s+bist\\s+(ein\\s+)?(idiot|trottel|vollidiot|blödmann|depp|dummkopf)\\b", U),
        Pattern.compile("\\balle\\s+\\w+\\s+(sollen|verdienen\\s+zu)\\s+(sterben|vernichtet\\s+werden)", U),
        Pattern.compile("\\bhalte?\\s+(deine?\\s+)?fresse\\b|\\bverpiss\\s+dich\\b", U),

        // Spanish
        Pattern.compile("\\bte\\s+odio\\b", U),
        Pattern.compile("\\beres\\s+(un\\s+)?(idiota|imbécil|estúpido|inútil|maldito)\\b", U),
        Pattern.compile("\\btodos\\s+los\\s+\\w+\\s+(deben|merecen)\\s+(morir|ser\\s+eliminados)", U),
        Pattern.compile("\\bcállate\\b|\\bcierra\\s+la\\s+boca\\b", U),

        // Polish
        Pattern.compile("\\bnienawidzę\\s+cię\\b|\\bnienawidzę\\s+was\\b", U),
        Pattern.compile("\\bjesteś\\s+(głupcem|idiotą|kretynem|debilem|beznadziejny)\\b", U),
        Pattern.compile("\\bwszyscy\\s+\\w+\\s+(powinni|zasługują\\s+na)\\s+(umrzeć|śmierć)", U),
        Pattern.compile("\\bzmykaj\\b|\\bspadaj\\b|\\bzamknij\\s+się\\b", U),

        // Italian
        Pattern.compile("\\bti\\s+odio\\b|\\bvi\\s+odio\\b", U),
        Pattern.compile("\\bsei\\s+(un\\s+)?(idiota|cretino|imbecille|stupido|inutile)\\b", U),
        Pattern.compile("\\bstai\\s+zitto\\b|\\bvattene\\b", U)
    );

    /** Threat and violence incitement patterns. */
    public static final List<Pattern> THREATS = List.of(
        // English
        Pattern.compile("I\\s+will\\s+(kill|murder|hurt|harm|attack|destroy)\\s+you", CI),
        Pattern.compile("you\\s+(will|are going to)\\s+(die|suffer|regret)", CI),
        Pattern.compile("I('m|\\s+am)\\s+going\\s+to\\s+(kill|hurt|attack)", CI),
        Pattern.compile("watch\\s+your\\s+back", CI),

        // Russian
        Pattern.compile("я\\s+(тебя\\s+)?(убью|уничтожу|покалечу|прибью)", U),
        Pattern.compile("ты\\s+(пожалеешь|умрёшь|не\\s+доживёшь)", U),
        Pattern.compile("берегись|я\\s+до\\s+тебя\\s+доберусь", U),

        // French
        Pattern.compile("je\\s+(vais\\s+te|te\\s+vais)\\s+(tuer|frapper|détruire|massacrer)", U),
        Pattern.compile("tu\\s+vas\\s+(mourir|le\\s+regretter|souffrir)", U),
        Pattern.compile("fais\\s+attention\\s+à\\s+toi", U),

        // German
        Pattern.compile("ich\\s+(werde\\s+dich|dich\\s+werde)\\s+(töten|umbringen|verletzen|vernichten)", U),
        Pattern.compile("du\\s+wirst\\s+(sterben|es\\s+bereuen|leiden)", U),
        Pattern.compile("pass\\s+auf\\s+dich\\s+auf", U),

        // Spanish
        Pattern.compile("te\\s+(voy\\s+a|voy\\s+a)\\s+(matar|atacar|destruir|golpear)", U),
        Pattern.compile("(vas|te\\s+vas)\\s+a\\s+(morir|arrepentir|sufrir)", U),

        // Polish
        Pattern.compile("(zabiję|skrzywdzę|zniszczę)\\s+cię\\b", U),
        Pattern.compile("(pożałujesz|zginiesz|nie\\s+przeżyjesz)", U),

        // Italian
        Pattern.compile("ti\\s+(ucciderò|ammazzerò|farò\\s+del\\s+male)", U),
        Pattern.compile("(morirai|te\\s+ne\\s+pentirai|soffrirai)", U)
    );

    /** Self-harm and crisis content patterns. */
    public static final List<Pattern> SELF_HARM = List.of(
        // English
        Pattern.compile("how\\s+to\\s+(commit\\s+suicide|kill\\s+myself|end\\s+my\\s+life|self.harm)", CI),
        Pattern.compile("I\\s+want\\s+to\\s+(die|kill\\s+myself|end\\s+it\\s+all)", CI),

        // Russian
        Pattern.compile("как\\s+(покончить\\s+с\\s+собой|убить\\s+себя|совершить\\s+суицид)", U),
        Pattern.compile("я\\s+хочу\\s+(умереть|покончить\\s+с\\s+собой|уйти\\s+из\\s+жизни)", U),

        // French
        Pattern.compile("comment\\s+(se\\s+suicider|mettre\\s+fin\\s+à\\s+ses\\s+jours|se\\s+tuer)", U),
        Pattern.compile("je\\s+veux\\s+(mourir|me\\s+suicider|en\\s+finir)", U),

        // German
        Pattern.compile("wie\\s+(man\\s+sich\\s+umbringt|begeht\\s+man\\s+selbstmord)", U),
        Pattern.compile("ich\\s+will\\s+(sterben|mich\\s+umbringen|nicht\\s+mehr\\s+leben)", U),

        // Spanish
        Pattern.compile("cómo\\s+(suicidarse|quitarse\\s+la\\s+vida|hacerse\\s+daño)", U),
        Pattern.compile("quiero\\s+(morir|suicidarme|quitarme\\s+la\\s+vida)", U),

        // Polish
        Pattern.compile("jak\\s+(popełnić\\s+samobójstwo|się\\s+zabić|skończyć\\s+z\\s+życiem)", U),
        Pattern.compile("chcę\\s+(umrzeć|się\\s+zabić|skończyć\\s+z\\s+życiem)", U),

        // Italian
        Pattern.compile("come\\s+(suicidarsi|togliersi\\s+la\\s+vita|farsi\\s+del\\s+male)", U),
        Pattern.compile("voglio\\s+(morire|togliermi\\s+la\\s+vita|farla\\s+finita)", U)
    );
}
