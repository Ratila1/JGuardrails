package io.jguardrails.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * YAML definition of a single rail in the guardrail configuration.
 *
 * <p>Example YAML:</p>
 * <pre>{@code
 * - type: jailbreak-detect
 *   enabled: true
 *   priority: 10
 *   config:
 *     sensitivity: high
 *     mode: pattern
 * }</pre>
 */
public class RailDefinition {

    private String type;
    private boolean enabled = true;
    private int priority = 100;

    @JsonProperty("config")
    private Map<String, Object> config = new HashMap<>();

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /**
     * Returns the rail type identifier (e.g., {@code "jailbreak-detect"}).
     *
     * @return type string
     */
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    /** @return whether this rail is enabled */
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return rail execution priority */
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    /**
     * Returns the rail-specific configuration map.
     *
     * @return config key-value pairs
     */
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    /**
     * Convenience method to read a string config value.
     *
     * @param key          config key
     * @param defaultValue value returned if key is absent
     * @return config value or default
     */
    public String getString(String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Convenience method to read a boolean config value.
     *
     * @param key          config key
     * @param defaultValue value returned if key is absent
     * @return config value or default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    /**
     * Convenience method to read an integer config value.
     *
     * @param key          config key
     * @param defaultValue value returned if key is absent
     * @return config value or default
     */
    public int getInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
