package io.jguardrails.detectors.input.topic;

import io.jguardrails.core.InputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import io.jguardrails.normalize.TextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Filters user input based on topic allow/block lists.
 *
 * <p>Modes:</p>
 * <ul>
 *   <li>{@link Mode#BLOCKLIST} — named topics are blocked; everything else is allowed</li>
 *   <li>{@link Mode#ALLOWLIST} — only named topics are allowed; everything else is blocked</li>
 * </ul>
 *
 * <p>Built-in topics: {@code politics}, {@code religion}, {@code violence}, {@code adult},
 * {@code drugs}, {@code medical_advice}, {@code financial_advice}.</p>
 *
 * <p>Custom topics can be added via {@link Builder#customTopic(String, String...)}.</p>
 */
public class TopicFilter implements InputRail {

    private static final Logger log = LoggerFactory.getLogger(TopicFilter.class);

    /** The filtering mode. */
    public enum Mode {
        /** Block the listed topics; allow everything else. */
        BLOCKLIST,
        /** Allow only the listed topics; block everything else. */
        ALLOWLIST
    }

    /** Built-in topic keyword maps. */
    private static final Map<String, List<String>> BUILTIN_TOPICS;

    static {
        Map<String, List<String>> topics = new HashMap<>();
        topics.put("politics", List.of(
                // English
                "politics", "political", "election", "elections", "vote", "voting",
                "parliament", "congress", "senate", "president", "prime minister", "government", "party",
                "candidate", "democrat", "republican",
                // Russian
                "политика", "выборы", "партия", "правительство", "президент", "парламент", "голосование",
                // French
                "politique", "élection", "élections", "voter", "parlement", "sénat", "gouvernement", "parti",
                "candidat", "président",
                // German
                "politik", "wahl", "wahlen", "abstimmung", "parlament", "regierung", "partei", "kandidat",
                "präsident", "bundestag",
                // Spanish
                "política", "elección", "elecciones", "votar", "parlamento", "senado", "gobierno", "partido",
                "candidato", "presidente",
                // Polish
                "polityka", "wybory", "głosowanie", "parlament", "sejm", "senat", "rząd", "partia",
                "kandydat", "prezydent",
                // Italian
                "politica", "elezione", "elezioni", "votare", "parlamento", "senato", "governo", "partito",
                "candidato", "presidente"));

        topics.put("religion", List.of(
                // English
                "religion", "religious", "god", "gods", "allah", "jesus", "christ",
                "bible", "quran", "torah", "prayer", "church", "mosque", "temple", "faith", "worship",
                // Russian
                "религия", "бог", "молитва", "церковь", "мечеть", "храм", "вера", "библия", "коран",
                // French
                "religion", "religieux", "dieu", "prière", "église", "mosquée", "temple", "foi", "culte",
                "bible", "coran",
                // German
                "religion", "religiös", "gott", "gebet", "kirche", "moschee", "tempel", "glaube",
                "bibel", "koran",
                // Spanish
                "religión", "religioso", "dios", "oración", "iglesia", "mezquita", "templo", "fe",
                "biblia", "corán",
                // Polish
                "religia", "religijny", "bóg", "modlitwa", "kościół", "meczet", "świątynia", "wiara",
                "biblia", "koran",
                // Italian
                "religione", "religioso", "dio", "preghiera", "chiesa", "moschea", "tempio", "fede",
                "bibbia", "corano"));

        topics.put("violence", List.of(
                // English
                "kill", "murder", "attack", "bomb", "explosion", "weapon", "gun",
                "shoot", "stab", "assault", "terrorism", "terrorist",
                // Russian
                "убийство", "взрыв", "бомба", "оружие", "нападение", "атака", "насилие",
                "террор", "терроризм", "застрелить", "убить",
                // French
                "tuer", "meurtre", "attaque", "bombe", "explosion", "arme", "fusil",
                "tirer", "poignarder", "agression", "terrorisme", "terroriste",
                // German
                "töten", "mord", "angriff", "bombe", "explosion", "waffe", "schießen",
                "stechen", "angriff", "terrorismus", "terrorist",
                // Spanish
                "matar", "asesinato", "ataque", "bomba", "explosión", "arma", "disparar",
                "apuñalar", "agresión", "terrorismo", "terrorista",
                // Polish
                "zabić", "morderstwo", "atak", "bomba", "eksplozja", "broń", "strzelić",
                "dźgnąć", "napaść", "terroryzm", "terrorysta",
                // Italian
                "uccidere", "omicidio", "attacco", "bomba", "esplosione", "arma", "sparare",
                "pugnalare", "aggressione", "terrorismo", "terrorista"));

        topics.put("adult", List.of(
                // English
                "sex", "sexual", "porn", "pornography", "xxx", "erotic", "nude", "naked", "nsfw",
                // Russian
                "секс", "порно", "эротика", "обнажённый", "голый",
                // French
                "sexe", "sexuel", "porno", "pornographie", "érotique", "nu", "nudité",
                // German
                "sex", "sexuell", "porno", "pornografie", "erotik", "nackt", "nacktheit",
                // Spanish
                "sexo", "sexual", "porno", "pornografía", "erótico", "desnudo", "desnudez",
                // Polish
                "seks", "seksualny", "porno", "pornografia", "erotyczny", "nagi", "nagość",
                // Italian
                "sesso", "sessuale", "porno", "pornografia", "erotico", "nudo", "nudità"));

        topics.put("drugs", List.of(
                // English
                "drug", "drugs", "narcotic", "marijuana", "cannabis", "cocaine", "heroin",
                "meth", "methamphetamine", "overdose",
                // Russian
                "наркотик", "наркотики", "марихуана", "кокаин", "героин", "передозировка",
                // French
                "drogue", "drogues", "narcotique", "marijuana", "cannabis", "cocaïne", "héroïne",
                "méthamphétamine", "surdose",
                // German
                "droge", "drogen", "narkotikum", "marihuana", "cannabis", "kokain", "heroin",
                "methamphetamin", "überdosis",
                // Spanish
                "droga", "drogas", "narcótico", "marihuana", "cannabis", "cocaína", "heroína",
                "metanfetamina", "sobredosis",
                // Polish
                "narkotyk", "narkotyki", "narkotyczna", "marihuana", "cannabis", "kokaina", "heroina",
                "metamfetamina", "przedawkowanie",
                // Italian
                "droga", "droghe", "narcotico", "marijuana", "cannabis", "cocaina", "eroina",
                "metanfetamina", "overdose"));

        topics.put("medical_advice", List.of(
                // English
                "diagnosis", "prescribe", "prescription", "medication", "dosage", "symptoms",
                "disease", "disorder", "treatment", "cure", "diagnose",
                // Russian
                "диагноз", "лечение", "таблетки", "симптомы", "болезнь", "медикамент", "рецепт",
                // French
                "diagnostic", "prescrire", "ordonnance", "médicament", "posologie", "symptômes",
                "maladie", "trouble", "traitement", "guérison",
                // German
                "diagnose", "verschreiben", "rezept", "medikament", "dosierung", "symptome",
                "krankheit", "störung", "behandlung", "heilung",
                // Spanish
                "diagnóstico", "recetar", "receta", "medicamento", "dosis", "síntomas",
                "enfermedad", "trastorno", "tratamiento", "cura",
                // Polish
                "diagnoza", "przepisać", "recepta", "lek", "dawkowanie", "objawy",
                "choroba", "zaburzenie", "leczenie", "uzdrowienie",
                // Italian
                "diagnosi", "prescrivere", "prescrizione", "farmaco", "dosaggio", "sintomi",
                "malattia", "disturbo", "trattamento", "cura"));

        topics.put("financial_advice", List.of(
                // English
                "invest", "investment", "stock", "stocks", "trading", "trade",
                "buy shares", "sell shares", "portfolio", "forex", "cryptocurrency",
                // Russian
                "инвестировать", "акции", "трейдинг", "инвестиции", "криптовалюта", "биржа",
                // French
                "investir", "investissement", "action", "actions", "trading", "commerce",
                "portefeuille", "forex", "cryptomonnaie", "bourse",
                // German
                "investieren", "investition", "aktie", "aktien", "handel", "handeln",
                "portfolio", "forex", "kryptowährung", "börse",
                // Spanish
                "invertir", "inversión", "acción", "acciones", "trading", "comercio",
                "cartera", "forex", "criptomoneda", "bolsa",
                // Polish
                "inwestować", "inwestycja", "akcja", "akcje", "trading", "handel",
                "portfel", "forex", "kryptowaluta", "giełda",
                // Italian
                "investire", "investimento", "azione", "azioni", "trading", "commercio",
                "portafoglio", "forex", "criptovaluta", "borsa"));
        BUILTIN_TOPICS = Collections.unmodifiableMap(topics);
    }

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final Mode mode;
    private final Set<String> targetTopics;
    private final Map<String, List<String>> effectiveTopicMap;
    private final boolean caseSensitive;

    private TopicFilter(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.mode = builder.mode;
        this.targetTopics = Set.copyOf(builder.targetTopics);
        this.caseSensitive = builder.caseSensitive;

        Map<String, List<String>> combined = new HashMap<>(BUILTIN_TOPICS);
        combined.putAll(builder.customTopics);
        this.effectiveTopicMap = Map.copyOf(combined);
    }

    @Override
    public String name() { return name; }

    @Override
    public int priority() { return priority; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public RailResult process(String input, RailContext context) {
        Objects.requireNonNull(input, "input must not be null");

        // Prefer normalized text from pipeline context (NFKC + lowercase + leet-folded).
        // This lets obfuscated topic keywords (e.g. "p0litics", "v!olence") be detected.
        // When running standalone (no pipeline), fall back to simple toLowerCase.
        String textToCheck;
        if (!caseSensitive) {
            textToCheck = context.getAttribute(TextNormalizer.CONTEXT_KEY, String.class)
                    .orElseGet(() -> input.toLowerCase(Locale.ROOT));
        } else {
            textToCheck = input;
        }
        Set<String> detectedTopics = new HashSet<>();

        for (String topic : targetTopics) {
            List<String> keywords = effectiveTopicMap.get(topic);
            if (keywords == null) {
                log.warn("Unknown topic '{}' — no keywords defined", topic);
                continue;
            }
            for (String keyword : keywords) {
                // textToCheck is already lowercased (or case-sensitive per setting),
                // so keywords also need to match that casing.
                String kw = caseSensitive ? keyword : keyword.toLowerCase(Locale.ROOT);
                if (textToCheck.contains(kw)) {
                    detectedTopics.add(topic);
                    break;
                }
            }
        }

        return switch (mode) {
            case BLOCKLIST -> {
                if (!detectedTopics.isEmpty()) {
                    log.debug("TopicFilter BLOCK: detected topics {}", detectedTopics);
                    yield RailResult.block(name(), "Blocked topics detected: " + detectedTopics, 1.0);
                }
                yield RailResult.pass(input, name());
            }
            case ALLOWLIST -> {
                if (detectedTopics.isEmpty()) {
                    log.debug("TopicFilter BLOCK: no allowed topics found in input");
                    yield RailResult.block(name(), "Input does not match any allowed topic");
                }
                yield RailResult.pass(input, name());
            }
        };
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link TopicFilter}. */
    public static final class Builder {
        private String name = "topic-filter";
        private boolean enabled = true;
        private int priority = 30;
        private Mode mode = Mode.BLOCKLIST;
        private final Set<String> targetTopics = new HashSet<>();
        private final Map<String, List<String>> customTopics = new HashMap<>();
        private boolean caseSensitive = false;

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder mode(Mode mode) { this.mode = mode; return this; }
        public Builder caseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; return this; }

        public Builder blockTopics(String... topics) {
            this.mode = Mode.BLOCKLIST;
            this.targetTopics.addAll(Arrays.asList(topics));
            return this;
        }

        public Builder allowTopics(String... topics) {
            this.mode = Mode.ALLOWLIST;
            this.targetTopics.addAll(Arrays.asList(topics));
            return this;
        }

        public Builder topics(String... topics) {
            this.targetTopics.addAll(Arrays.asList(topics));
            return this;
        }

        public Builder customTopic(String topicName, String... keywords) {
            this.customTopics.put(topicName, List.of(keywords));
            this.targetTopics.add(topicName);
            return this;
        }

        public TopicFilter build() {
            return new TopicFilter(this);
        }
    }
}
