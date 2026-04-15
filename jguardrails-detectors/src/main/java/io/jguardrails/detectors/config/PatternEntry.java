package io.jguardrails.detectors.config;

/**
 * A single pattern definition as declared in a YAML configuration file.
 *
 * <p>Fields map directly to the YAML keys:</p>
 * <pre>{@code
 * - id: HUMAN_READABLE_ID
 *   flags: CI          # CI | U | UC | NONE  (defaults to CI when absent)
 *   pattern: "\\bexample\\b"
 * }</pre>
 *
 * @param id      Human-readable identifier used in logs and diagnostics.
 * @param pattern The regex string (not yet compiled).
 * @param flags   Compilation-flag abbreviation; see {@link PatternLoader#parseFlags(String)}.
 */
public record PatternEntry(String id, String pattern, String flags) {}
