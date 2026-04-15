package io.jguardrails.detectors.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link TextPatternEngine} implementation backed by an Aho-Corasick automaton for
 * efficient multi-keyword literal matching.
 *
 * <p>All keywords are stored in a single trie with failure links, enabling text search in
 * O(n + m + z) time — where n is the text length, m is the total length of all keywords, and
 * z is the number of matches — regardless of how many keywords are registered.  This is
 * significantly more efficient than running k separate {@code indexOf} calls or regex patterns
 * when k is large.</p>
 *
 * <h2>Case sensitivity</h2>
 * <p>Matching is always <strong>case-insensitive</strong>: keywords are lowercased at build time
 * and the search text is lowercased on the fly.  The original casing of the matched substring is
 * preserved in {@link MatchResult#matchedText()}.</p>
 *
 * <h2>Construction</h2>
 * <pre>{@code
 * KeywordAutomatonEngine engine = new KeywordAutomatonEngine(Map.of(
 *     "KW_IGNORE_SYSTEM_PROMPT", "ignore the system prompt",
 *     "KW_DO_ANYTHING_NOW",      "do anything now"
 * ));
 * }</pre>
 *
 * <p>Or use {@link io.jguardrails.detectors.config.PatternLoader#buildKeywordEngine} to load
 * all {@code type: KEYWORD} entries from a YAML resource file.</p>
 *
 * <h2>Thread safety</h2>
 * <p>Instances are immutable after construction and fully thread-safe.</p>
 */
public final class KeywordAutomatonEngine implements TextPatternEngine {

    // ── Aho-Corasick state node ───────────────────────────────────────────────

    private static final class AcNode {
        /** Trie child transitions (character → next state). */
        final Map<Character, AcNode> children = new HashMap<>();
        /** Failure link (longest proper suffix that is a prefix of any keyword). */
        AcNode fail;
        /** Spec ids whose keyword ends at this node (including via suffix links). */
        final List<String> output = new ArrayList<>();
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Spec id → lowercased keyword. */
    private final Map<String, String> specIdToKeyword;

    /** Root of the Aho-Corasick automaton. */
    private final AcNode root;

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Builds an Aho-Corasick automaton over the given spec-id → keyword map.
     * Keywords are lowercased at build time for case-insensitive matching.
     *
     * @param specIdToKeyword map from pattern id to literal keyword / phrase; must not be null
     */
    public KeywordAutomatonEngine(Map<String, String> specIdToKeyword) {
        Objects.requireNonNull(specIdToKeyword, "specIdToKeyword must not be null");
        // Normalise keywords to lowercase, preserving insertion order
        Map<String, String> normalised = new LinkedHashMap<>(specIdToKeyword.size());
        for (Map.Entry<String, String> e : specIdToKeyword.entrySet()) {
            normalised.put(e.getKey(), e.getValue().toLowerCase(Locale.ROOT));
        }
        this.specIdToKeyword = Map.copyOf(normalised);
        this.root = buildAutomaton(this.specIdToKeyword);
    }

    // ── Automaton construction ────────────────────────────────────────────────

    private static AcNode buildAutomaton(Map<String, String> idToKeyword) {
        AcNode root = new AcNode();

        // Phase 1: insert all keywords into the trie
        for (Map.Entry<String, String> e : idToKeyword.entrySet()) {
            insertKeyword(root, e.getValue(), e.getKey());
        }

        // Phase 2: BFS to build failure links and propagate output sets
        root.fail = root;
        Queue<AcNode> queue = new ArrayDeque<>();
        for (AcNode child : root.children.values()) {
            child.fail = root;
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            AcNode cur = queue.poll();
            for (Map.Entry<Character, AcNode> e : cur.children.entrySet()) {
                char c = e.getKey();
                AcNode child = e.getValue();

                // Follow failure links of parent to find child's failure state
                AcNode f = cur.fail;
                while (f != root && !f.children.containsKey(c)) {
                    f = f.fail;
                }
                child.fail = f.children.getOrDefault(c, root);
                if (child.fail == child) child.fail = root; // guard against root self-loop

                // Inherit output from failure state (dictionary suffix links)
                child.output.addAll(child.fail.output);
                queue.add(child);
            }
        }
        return root;
    }

    private static void insertKeyword(AcNode root, String keyword, String specId) {
        AcNode cur = root;
        for (char c : keyword.toCharArray()) {
            cur = cur.children.computeIfAbsent(c, ignored -> new AcNode());
        }
        cur.output.add(specId);
    }

    // ── TextPatternEngine ─────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #find} — uses Aho-Corasick for a single pattern only.</p>
     */
    @Override
    public boolean matches(String text, PatternSpec spec) {
        return find(text, spec).matched();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Searches for the single keyword associated with {@code spec.id()} via
     * {@link String#indexOf} on the lowercased text.  Use {@link #findFirst(String, List)} to
     * leverage the full Aho-Corasick multi-keyword speedup.</p>
     *
     * @throws IllegalArgumentException if the spec id is not registered in this engine
     */
    @Override
    public MatchResult find(String text, PatternSpec spec) {
        String keyword = resolve(spec);
        String searchText = text.toLowerCase(Locale.ROOT);
        int idx = searchText.indexOf(keyword);
        if (idx < 0) return MatchResult.NO_MATCH;
        return MatchResult.of(text.substring(idx, idx + keyword.length()), idx, idx + keyword.length());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Runs the Aho-Corasick automaton over the entire text in a <strong>single pass</strong>,
     * finding the first match among all relevant specs in O(n + m) time — where n is the text
     * length and m is the total keyword length.  This is the primary performance advantage over
     * calling {@link #find} per spec.</p>
     */
    @Override
    public Optional<MatchedSpec> findFirst(String text, List<PatternSpec> specs) {
        if (specs.isEmpty()) return Optional.empty();

        Set<String> relevantIds = specs.stream()
                .map(PatternSpec::id)
                .collect(Collectors.toSet());
        Map<String, PatternSpec> specById = specs.stream()
                .collect(Collectors.toMap(PatternSpec::id, s -> s, (a, b) -> a));

        String searchText = text.toLowerCase(Locale.ROOT);
        AcNode state = root;
        for (int i = 0; i < searchText.length(); i++) {
            char c = searchText.charAt(i);

            // Follow failure links until a valid transition is found
            while (state != root && !state.children.containsKey(c)) {
                state = state.fail;
            }
            state = state.children.getOrDefault(c, root);

            for (String specId : state.output) {
                if (!relevantIds.contains(specId)) continue;
                String keyword = specIdToKeyword.get(specId);
                int end   = i + 1;
                int start = end - keyword.length();
                return Optional.of(new MatchedSpec(
                        specById.get(specId),
                        MatchResult.of(text.substring(start, end), start, end)));
            }
        }
        return Optional.empty();
    }

    // ── Introspection ─────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable view of the spec-id → keyword map held by this engine.
     * Keywords are stored in lowercase form.
     */
    public Map<String, String> keywords() {
        return specIdToKeyword; // already Map.copyOf — immutable
    }

    /** Returns {@code true} if a keyword is registered under {@code id}. */
    public boolean containsId(String id) {
        return specIdToKeyword.containsKey(id);
    }

    /** Returns the number of keywords registered in this engine. */
    public int size() {
        return specIdToKeyword.size();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolve(PatternSpec spec) {
        String keyword = specIdToKeyword.get(spec.id());
        if (keyword == null) {
            throw new IllegalArgumentException(
                    "KeywordAutomatonEngine: unknown spec id '" + spec.id()
                    + "' (category='" + spec.category() + "'). "
                    + "Ensure the engine was built with all keyword specs referenced by the active spec list.");
        }
        return keyword;
    }
}
