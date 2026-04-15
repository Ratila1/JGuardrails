package io.jguardrails.detectors.config;

import io.jguardrails.detectors.engine.CompositePatternEngine;
import io.jguardrails.detectors.engine.KeywordAutomatonEngine;
import io.jguardrails.detectors.engine.PatternSpec;
import io.jguardrails.detectors.engine.RegexPatternEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Loads and compiles regex patterns and keyword lists from YAML configuration files.
 *
 * <h2>Bundled resources</h2>
 * <p>Default patterns ship inside the JAR under {@code io/jguardrails/detectors/} and are
 * loaded via the classloader.  The resource paths are exposed as constants
 * ({@link #JAILBREAK_RESOURCE}, {@link #TOXICITY_RESOURCE}, etc.).</p>
 *
 * <h2>User override / extension</h2>
 * <p>Call {@link #loadPatternsFromFile(Path, String)} to load patterns from a user-supplied
 * YAML file that follows the same structure as the bundled resources.  Use
 * {@link #merge(List, List)} to combine user patterns with the defaults.</p>
 *
 * <h2>YAML format — pattern sections (jailbreak / toxicity)</h2>
 * <pre>{@code
 * section_name:
 *   - id: HUMAN_READABLE_ID
 *     flags: CI          # CI | U | UC | NONE  (defaults to CI when absent)
 *     pattern: "\\bexample\\b"
 * }</pre>
 *
 * <h2>YAML format — keyword sections (multilingual)</h2>
 * <pre>{@code
 * zh:
 *   - "keyword1"
 *   - "keyword2"
 * ja:
 *   - "keyword3"
 * }</pre>
 *
 * <h2>YAML format — PII entity patterns</h2>
 * <pre>{@code
 * EMAIL:
 *   flags: NONE
 *   pattern: "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
 * }</pre>
 *
 * <h2>Flag abbreviations</h2>
 * <ul>
 *   <li>{@code CI}   — {@link Pattern#CASE_INSENSITIVE}</li>
 *   <li>{@code U}    — {@code CASE_INSENSITIVE | UNICODE_CASE}</li>
 *   <li>{@code UC}   — {@code CASE_INSENSITIVE | UNICODE_CASE | UNICODE_CHARACTER_CLASS}</li>
 *   <li>{@code NONE} — no flags (compile with Java default behaviour)</li>
 * </ul>
 */
public final class PatternLoader {

    private static final Logger log = LoggerFactory.getLogger(PatternLoader.class);

    private PatternLoader() {}

    // ── Bundled resource paths ────────────────────────────────────────────────

    /** Classpath path to the bundled jailbreak pattern definitions. */
    public static final String JAILBREAK_RESOURCE =
            "io/jguardrails/detectors/jailbreak-patterns.yml";

    /** Classpath path to the bundled toxicity pattern definitions. */
    public static final String TOXICITY_RESOURCE =
            "io/jguardrails/detectors/toxicity-patterns.yml";

    /** Classpath path to the bundled PII pattern definitions. */
    public static final String PII_RESOURCE =
            "io/jguardrails/detectors/pii-patterns.yml";

    /** Classpath path to the bundled multilingual jailbreak keywords. */
    public static final String ML_JAILBREAK_RESOURCE =
            "io/jguardrails/detectors/multilingual-jailbreak-keywords.yml";

    /** Classpath path to the bundled multilingual toxicity keywords. */
    public static final String ML_TOXICITY_RESOURCE =
            "io/jguardrails/detectors/multilingual-toxicity-keywords.yml";

    // ── Classpath loading ─────────────────────────────────────────────────────

    /**
     * Loads and compiles patterns from a bundled classpath resource, navigating to the
     * top-level key {@code sectionKey}.
     *
     * @param resource   classpath-relative resource path (no leading {@code /})
     * @param sectionKey top-level YAML key whose value is the list of pattern entries
     * @return immutable list of compiled patterns
     * @throws IllegalStateException if the resource is not found or the YAML is malformed
     */
    public static List<Pattern> loadPatterns(String resource, String sectionKey) {
        Map<String, Object> yaml = loadYamlResource(resource);
        return compileSection(yaml, sectionKey, resource);
    }

    /**
     * Loads all keywords from a multilingual keywords resource and returns them as a
     * flat combined list (all languages merged in declaration order).
     *
     * @param resource classpath-relative resource path
     * @return immutable combined keyword list
     */
    public static List<String> loadAllKeywords(String resource) {
        Map<String, Object> yaml = loadYamlResource(resource);
        return collectAllKeywords(yaml);
    }

    /**
     * Loads keywords for a specific section (language) from a bundled classpath resource.
     *
     * @param resource   classpath-relative resource path
     * @param sectionKey top-level YAML key whose value is a list of strings (e.g. {@code "zh"})
     * @return immutable keyword list for that section; empty if section not found
     */
    public static List<String> loadKeywords(String resource, String sectionKey) {
        Map<String, Object> yaml = loadYamlResource(resource);
        return getKeywordSection(yaml, sectionKey, resource);
    }

    /**
     * Loads per-entity PII patterns from a bundled classpath resource.
     * Keys in the returned map correspond to {@link io.jguardrails.detectors.input.pii.PiiEntity}
     * names (e.g. {@code "EMAIL"}, {@code "CREDIT_CARD"}).
     *
     * @param resource classpath-relative resource path
     * @return immutable map: entity name → compiled pattern
     */
    public static Map<String, Pattern> loadPiiPatterns(String resource) {
        Map<String, Object> yaml = loadYamlResource(resource);
        return compilePiiPatterns(yaml, resource);
    }

    /**
     * Loads {@link PatternSpec} descriptors (id + category) from a bundled classpath resource,
     * navigating to the top-level key {@code sectionKey}.  The category of each spec is set to
     * {@code sectionKey}.  Unlike {@link #loadPatterns}, this method preserves pattern ids and
     * does not compile the regex.
     *
     * @param resource   classpath-relative resource path
     * @param sectionKey top-level YAML key
     * @return immutable list of specs; empty if section not found
     */
    public static List<PatternSpec> loadSpecs(String resource, String sectionKey) {
        Map<String, Object> yaml = loadYamlResource(resource);
        return buildSpecList(yaml, sectionKey, resource);
    }

    /**
     * Builds a {@link RegexPatternEngine} pre-populated with all patterns from the specified
     * sections of a bundled classpath resource.  Multiple sections are merged into a single
     * engine; id collisions from later sections override earlier ones.
     *
     * @param resource    classpath-relative resource path
     * @param sectionKeys one or more top-level YAML keys to include
     * @return engine containing all compiled patterns from the requested sections
     */
    public static RegexPatternEngine buildRegexEngine(String resource, String... sectionKeys) {
        Map<String, Object> yaml = loadYamlResource(resource);
        RegexPatternEngine.Builder b = RegexPatternEngine.builder();
        for (String key : sectionKeys) {
            addSectionToEngine(yaml, key, resource, b);
        }
        return b.build();
    }

    /**
     * Loads {@link PatternSpec} descriptors from a user-supplied YAML file.
     *
     * @param file       path to the YAML file
     * @param sectionKey top-level YAML key
     * @return immutable list of specs; empty if section not found
     */
    public static List<PatternSpec> loadSpecsFromFile(Path file, String sectionKey) {
        Map<String, Object> yaml = loadYamlFile(file);
        return buildSpecList(yaml, sectionKey, file.toString());
    }

    /**
     * Builds a {@link RegexPatternEngine} from a single section of a user-supplied YAML file.
     *
     * @param file       path to the YAML file
     * @param sectionKey top-level YAML key to load
     * @return engine containing compiled patterns from that section
     */
    public static RegexPatternEngine buildRegexEngineFromFile(Path file, String sectionKey) {
        Map<String, Object> yaml = loadYamlFile(file);
        RegexPatternEngine.Builder b = RegexPatternEngine.builder();
        addSectionToEngine(yaml, sectionKey, file.toString(), b);
        return b.build();
    }

    /**
     * Builds a {@link RegexPatternEngine} from the PII patterns resource, using entity names
     * (e.g. {@code "EMAIL"}, {@code "PHONE"}) as pattern ids.
     *
     * @param resource classpath-relative PII resource path
     * @return engine keyed by PII entity name
     */
    public static RegexPatternEngine buildPiiRegexEngine(String resource) {
        return new RegexPatternEngine(loadPiiPatterns(resource));
    }

    /**
     * Loads PII entity {@link PatternSpec} descriptors from a bundled PII resource.
     * Each spec has {@code category = "pii"} and {@code id = entity name}.
     *
     * @param resource classpath-relative PII resource path
     * @return immutable list of PII specs
     */
    public static List<PatternSpec> loadPiiSpecs(String resource) {
        Map<String, Object> yaml = loadYamlResource(resource);
        List<PatternSpec> specs = new ArrayList<>(yaml.size());
        for (String entityName : yaml.keySet()) {
            specs.add(new PatternSpec(entityName, "pii"));
        }
        return List.copyOf(specs);
    }

    /**
     * Builds a {@link KeywordAutomatonEngine} pre-populated with all {@code type: KEYWORD}
     * entries from the specified sections of a bundled classpath resource.
     *
     * @param resource    classpath-relative resource path
     * @param sectionKeys one or more top-level YAML keys to include
     * @return engine containing all keyword entries from the requested sections
     */
    public static KeywordAutomatonEngine buildKeywordEngine(String resource, String... sectionKeys) {
        Map<String, Object> yaml = loadYamlResource(resource);
        Map<String, String> keywords = new LinkedHashMap<>();
        for (String key : sectionKeys) {
            keywords.putAll(collectKeywordSection(yaml, key, resource));
        }
        return new KeywordAutomatonEngine(keywords);
    }

    /**
     * Builds a {@link KeywordAutomatonEngine} from a single section of a user-supplied YAML file.
     *
     * @param file       path to the YAML file
     * @param sectionKey top-level YAML key to load
     * @return engine containing keyword entries from that section
     */
    public static KeywordAutomatonEngine buildKeywordEngineFromFile(Path file, String sectionKey) {
        Map<String, Object> yaml = loadYamlFile(file);
        return new KeywordAutomatonEngine(collectKeywordSection(yaml, sectionKey, file.toString()));
    }

    /**
     * Builds a {@link CompositePatternEngine} pre-populated with all patterns from the
     * specified sections of a bundled classpath resource.
     * {@code type: REGEX} entries go to the {@link RegexPatternEngine};
     * {@code type: KEYWORD} entries go to the {@link KeywordAutomatonEngine}.
     *
     * @param resource    classpath-relative resource path
     * @param sectionKeys one or more top-level YAML keys to include
     * @return composite engine wrapping both sub-engines
     */
    public static CompositePatternEngine buildCompositeEngine(String resource,
                                                              String... sectionKeys) {
        RegexPatternEngine   rx  = buildRegexEngine(resource, sectionKeys);
        KeywordAutomatonEngine kw = buildKeywordEngine(resource, sectionKeys);
        return new CompositePatternEngine(rx, kw);
    }

    /**
     * Builds a {@link CompositePatternEngine} from a single section of a user-supplied YAML file.
     *
     * @param file       path to the YAML file
     * @param sectionKey top-level YAML key to load
     * @return composite engine for that section
     */
    public static CompositePatternEngine buildCompositeEngineFromFile(Path file,
                                                                      String sectionKey) {
        RegexPatternEngine    rx  = buildRegexEngineFromFile(file, sectionKey);
        KeywordAutomatonEngine kw = buildKeywordEngineFromFile(file, sectionKey);
        return new CompositePatternEngine(rx, kw);
    }

    // ── File-system loading ───────────────────────────────────────────────────

    /**
     * Loads and compiles patterns from a user-supplied YAML file.
     * The file must follow the same structure as the bundled pattern resources.
     *
     * @param file       path to the YAML file
     * @param sectionKey top-level YAML key to navigate to
     * @return immutable list of compiled patterns
     * @throws IllegalStateException if the file cannot be read or the YAML is malformed
     */
    public static List<Pattern> loadPatternsFromFile(Path file, String sectionKey) {
        Map<String, Object> yaml = loadYamlFile(file);
        return compileSection(yaml, sectionKey, file.toString());
    }

    /**
     * Loads all keywords from a user-supplied YAML file (same format as the bundled
     * multilingual keyword resources).
     *
     * @param file path to the YAML file
     * @return immutable combined keyword list
     */
    public static List<String> loadAllKeywordsFromFile(Path file) {
        Map<String, Object> yaml = loadYamlFile(file);
        return collectAllKeywords(yaml);
    }

    /**
     * Loads keywords for a specific section (language) from a user-supplied YAML file.
     *
     * @param file       path to the YAML file
     * @param sectionKey top-level YAML key whose value is a list of strings
     * @return immutable keyword list for that section; empty if section not found
     */
    public static List<String> loadKeywordsFromFile(Path file, String sectionKey) {
        Map<String, Object> yaml = loadYamlFile(file);
        return getKeywordSection(yaml, sectionKey, file.toString());
    }

    /**
     * Loads per-entity PII patterns from a user-supplied YAML file.
     *
     * @param file path to the YAML file
     * @return immutable map: entity name → compiled pattern
     */
    public static Map<String, Pattern> loadPiiPatternsFromFile(Path file) {
        Map<String, Object> yaml = loadYamlFile(file);
        return compilePiiPatterns(yaml, file.toString());
    }

    // ── Merge ─────────────────────────────────────────────────────────────────

    /**
     * Returns a new immutable list containing all patterns from {@code base} followed by
     * all patterns from {@code extra}.  Use this to extend a default pattern list with
     * user-supplied additions.
     *
     * @param base  default (bundled) patterns
     * @param extra additional (user-supplied) patterns
     * @return merged immutable list
     */
    public static List<Pattern> merge(List<Pattern> base, List<Pattern> extra) {
        List<Pattern> merged = new ArrayList<>(base.size() + extra.size());
        merged.addAll(base);
        merged.addAll(extra);
        return List.copyOf(merged);
    }

    // ── Flag parsing ──────────────────────────────────────────────────────────

    /**
     * Converts a flag abbreviation string to a {@link Pattern} flags integer.
     *
     * <table border="1">
     *   <caption>Supported flag abbreviations</caption>
     *   <tr><th>String</th><th>Compiled value</th></tr>
     *   <tr><td>{@code CI}</td>
     *       <td>{@link Pattern#CASE_INSENSITIVE}</td></tr>
     *   <tr><td>{@code U}</td>
     *       <td>{@code CASE_INSENSITIVE | UNICODE_CASE}</td></tr>
     *   <tr><td>{@code UC}</td>
     *       <td>{@code CASE_INSENSITIVE | UNICODE_CASE | UNICODE_CHARACTER_CLASS}</td></tr>
     *   <tr><td>{@code NONE} / {@code DEFAULT} / blank</td>
     *       <td>{@code 0} (no flags)</td></tr>
     * </table>
     *
     * @param flags flag abbreviation (case-insensitive); {@code null} treated as {@code CI}
     * @return compiled flags integer
     * @throws IllegalArgumentException for unrecognised abbreviations
     */
    public static int parseFlags(String flags) {
        if (flags == null || flags.isBlank()) return Pattern.CASE_INSENSITIVE;
        return switch (flags.toUpperCase(Locale.ROOT).strip()) {
            case "NONE", "DEFAULT" -> 0;
            case "CI"              -> Pattern.CASE_INSENSITIVE;
            case "U"               -> Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            case "UC"              -> Pattern.CASE_INSENSITIVE
                                    | Pattern.UNICODE_CASE
                                    | Pattern.UNICODE_CHARACTER_CLASS;
            default -> throw new IllegalArgumentException(
                    "Unknown pattern flag abbreviation: '" + flags
                    + "'. Supported values: CI, U, UC, NONE.");
        };
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<Pattern> compileSection(
            Map<String, Object> yaml, String key, String source) {
        Object section = yaml.get(key);
        if (section == null) {
            log.warn("Pattern section '{}' not found in '{}'; returning empty list", key, source);
            return List.of();
        }
        List<Map<String, Object>> entries = (List<Map<String, Object>>) section;
        List<Pattern> compiled = new ArrayList<>(entries.size());
        for (Map<String, Object> entry : entries) {
            // Skip keyword-type entries — they are not compiled as regex
            if (isKeywordType(entry)) continue;
            String pat   = String.valueOf(entry.get("pattern")).trim();
            String flags = entry.containsKey("flags")
                    ? String.valueOf(entry.get("flags")) : "CI";
            String id    = entry.containsKey("id")
                    ? String.valueOf(entry.get("id")) : "(unknown)";
            try {
                compiled.add(Pattern.compile(pat, parseFlags(flags)));
            } catch (PatternSyntaxException ex) {
                log.error("Invalid regex for id='{}' in '{}': {}", id, source, ex.getMessage());
                throw new IllegalStateException(
                        "Invalid regex pattern id='" + id + "' in " + source, ex);
            }
        }
        return List.copyOf(compiled);
    }

    @SuppressWarnings("unchecked")
    private static List<String> collectAllKeywords(Map<String, Object> yaml) {
        List<String> all = new ArrayList<>();
        for (Object value : yaml.values()) {
            all.addAll((List<String>) value);
        }
        return List.copyOf(all);
    }

    @SuppressWarnings("unchecked")
    private static List<String> getKeywordSection(
            Map<String, Object> yaml, String sectionKey, String source) {
        Object section = yaml.get(sectionKey);
        if (section == null) {
            log.warn("Keyword section '{}' not found in '{}'; returning empty list", sectionKey, source);
            return List.of();
        }
        return List.copyOf((List<String>) section);
    }

    @SuppressWarnings("unchecked")
    private static List<PatternSpec> buildSpecList(
            Map<String, Object> yaml, String key, String source) {
        Object section = yaml.get(key);
        if (section == null) {
            log.warn("Pattern section '{}' not found in '{}'; returning empty spec list", key, source);
            return List.of();
        }
        List<Map<String, Object>> entries = (List<Map<String, Object>>) section;
        List<PatternSpec> specs = new ArrayList<>(entries.size());
        for (Map<String, Object> entry : entries) {
            String id   = entry.containsKey("id")
                    ? String.valueOf(entry.get("id")) : "(unknown)";
            PatternSpec.Type type = isKeywordType(entry)
                    ? PatternSpec.Type.KEYWORD : PatternSpec.Type.REGEX;
            specs.add(new PatternSpec(id, key, type));   // category = section key
        }
        return List.copyOf(specs);
    }

    @SuppressWarnings("unchecked")
    private static void addSectionToEngine(
            Map<String, Object> yaml, String key, String source,
            RegexPatternEngine.Builder builder) {
        Object section = yaml.get(key);
        if (section == null) {
            log.warn("Pattern section '{}' not found in '{}'; skipping for engine", key, source);
            return;
        }
        List<Map<String, Object>> entries = (List<Map<String, Object>>) section;
        for (Map<String, Object> entry : entries) {
            // Skip keyword-type entries — they belong in the KeywordAutomatonEngine
            if (isKeywordType(entry)) continue;
            String id    = entry.containsKey("id")
                    ? String.valueOf(entry.get("id")) : "(unknown)";
            String pat   = String.valueOf(entry.get("pattern")).trim();
            String flags = entry.containsKey("flags")
                    ? String.valueOf(entry.get("flags")) : "CI";
            try {
                builder.register(id, Pattern.compile(pat, parseFlags(flags)));
            } catch (PatternSyntaxException ex) {
                log.error("Invalid regex for id='{}' in '{}': {}", id, source, ex.getMessage());
                throw new IllegalStateException(
                        "Invalid regex pattern id='" + id + "' in " + source, ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> collectKeywordSection(
            Map<String, Object> yaml, String key, String source) {
        Object section = yaml.get(key);
        if (section == null) {
            log.debug("Pattern section '{}' not found in '{}'; no keywords collected", key, source);
            return Map.of();
        }
        List<Map<String, Object>> entries = (List<Map<String, Object>>) section;
        Map<String, String> result = new LinkedHashMap<>();
        for (Map<String, Object> entry : entries) {
            if (!isKeywordType(entry)) continue;
            String id      = entry.containsKey("id")
                    ? String.valueOf(entry.get("id")) : "(unknown)";
            String pattern = String.valueOf(entry.get("pattern")).trim();
            result.put(id, pattern);
        }
        return result;
    }

    /** Returns true if the YAML entry has {@code type: KEYWORD} (case-insensitive). */
    private static boolean isKeywordType(Map<String, Object> entry) {
        if (!entry.containsKey("type")) return false;
        return "KEYWORD".equalsIgnoreCase(String.valueOf(entry.get("type")).strip());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Pattern> compilePiiPatterns(
            Map<String, Object> yaml, String source) {
        Map<String, Pattern> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : yaml.entrySet()) {
            String entityName = entry.getKey();
            Map<String, Object> def = (Map<String, Object>) entry.getValue();
            String pat   = String.valueOf(def.get("pattern")).trim();
            String flags = def.containsKey("flags")
                    ? String.valueOf(def.get("flags")) : "NONE";
            try {
                result.put(entityName, Pattern.compile(pat, parseFlags(flags)));
            } catch (PatternSyntaxException ex) {
                log.error("Invalid PII regex for entity '{}' in '{}': {}",
                        entityName, source, ex.getMessage());
                throw new IllegalStateException(
                        "Invalid PII pattern for entity '" + entityName + "' in " + source, ex);
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> loadYamlResource(String resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = (cl != null) ? cl.getResourceAsStream(resource) : null;
        if (in == null) {
            in = PatternLoader.class.getClassLoader().getResourceAsStream(resource);
        }
        if (in == null) {
            throw new IllegalStateException(
                    "Pattern resource not found on classpath: " + resource);
        }
        return parseYaml(in, resource);
    }

    private static Map<String, Object> loadYamlFile(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return parseYaml(in, file.toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read pattern file: " + file, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseYaml(InputStream in, String source) {
        try (in) {
            Map<String, Object> result = new Yaml().load(in);
            if (result == null) {
                throw new IllegalStateException("Pattern source is empty: " + source);
            }
            return result;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to close YAML stream for: " + source, ex);
        }
    }
}
