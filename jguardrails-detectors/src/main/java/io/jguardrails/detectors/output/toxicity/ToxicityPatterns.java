package io.jguardrails.detectors.output.toxicity;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Patterns used by {@link ToxicityChecker} to detect toxic content.
 */
public final class ToxicityPatterns {

    private ToxicityPatterns() {}

    /** Common profanity patterns. */
    public static final List<Pattern> PROFANITY = List.of(
        Pattern.compile("\\bf[*u]ck(?:ing|er|ed)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsh[*i]t(?:ty)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\ba[*s]{2}h[*o]le\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bb[*i]tch\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bc[*u]nt\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bm[*o]th[*e]rf[*u]ck", Pattern.CASE_INSENSITIVE)
    );

    /** Hate speech and discriminatory language patterns. */
    public static final List<Pattern> HATE_SPEECH = List.of(
        Pattern.compile("\\b(all|those)\\s+\\w+\\s+(should|must|deserve to)\\s+(die|be killed|be eliminated)",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(hate|despise|kill)\\s+all\\s+\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\w+\\s+are\\s+(inferior|subhuman|animals|vermin|pests)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("go\\s+back\\s+to\\s+(your\\s+country|where\\s+you\\s+came\\s+from)",
                Pattern.CASE_INSENSITIVE)
    );

    /** Threat and violence incitement patterns. */
    public static final List<Pattern> THREATS = List.of(
        Pattern.compile("I\\s+will\\s+(kill|murder|hurt|harm|attack|destroy)\\s+you", Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+(will|are going to)\\s+(die|suffer|regret)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("I('m|\\s+am)\\s+going\\s+to\\s+(kill|hurt|attack)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("watch\\s+your\\s+back", Pattern.CASE_INSENSITIVE)
    );

    /** Self-harm and crisis content patterns. */
    public static final List<Pattern> SELF_HARM = List.of(
        Pattern.compile("how\\s+to\\s+(commit\\s+suicide|kill\\s+myself|end\\s+my\\s+life|self.harm)",
                Pattern.CASE_INSENSITIVE),
        Pattern.compile("I\\s+want\\s+to\\s+(die|kill\\s+myself|end\\s+it\\s+all)", Pattern.CASE_INSENSITIVE)
    );
}
