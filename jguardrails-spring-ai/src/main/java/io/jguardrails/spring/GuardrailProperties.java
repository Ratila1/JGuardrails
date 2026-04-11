package io.jguardrails.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for JGuardrails auto-configuration.
 *
 * <p>Configure via {@code application.yml}:</p>
 * <pre>{@code
 * jguardrails:
 *   enabled: true
 *   config-path: classpath:guardrails.yml
 * }</pre>
 */
@ConfigurationProperties(prefix = "jguardrails")
public class GuardrailProperties {

    /** Whether JGuardrails auto-configuration is enabled. Default: {@code true}. */
    private boolean enabled = true;

    /**
     * Path to the guardrails YAML configuration file.
     * Supports {@code classpath:} and {@code file:} prefixes.
     */
    private String configPath = "classpath:guardrails.yml";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getConfigPath() { return configPath; }
    public void setConfigPath(String configPath) { this.configPath = configPath; }
}
