package io.jguardrails.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Root YAML configuration model for JGuardrails.
 *
 * <p>Maps to the {@code jguardrails} key in {@code guardrails.yml}.</p>
 */
public class GuardrailConfig {

    @JsonProperty("fail-strategy")
    private String failStrategy = "closed";

    @JsonProperty("blocked-response")
    private String blockedResponse = "I'm unable to process this request.";

    @JsonProperty("llm-judge")
    private LlmJudgeConfig llmJudge;

    @JsonProperty("input-rails")
    private List<RailDefinition> inputRails = new ArrayList<>();

    @JsonProperty("output-rails")
    private List<RailDefinition> outputRails = new ArrayList<>();

    @JsonProperty("audit")
    private AuditConfig audit = new AuditConfig();

    @JsonProperty("metrics")
    private MetricsConfig metrics = new MetricsConfig();

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getFailStrategy() { return failStrategy; }
    public void setFailStrategy(String failStrategy) { this.failStrategy = failStrategy; }

    public String getBlockedResponse() { return blockedResponse; }
    public void setBlockedResponse(String blockedResponse) { this.blockedResponse = blockedResponse; }

    public LlmJudgeConfig getLlmJudge() { return llmJudge; }
    public void setLlmJudge(LlmJudgeConfig llmJudge) { this.llmJudge = llmJudge; }

    public List<RailDefinition> getInputRails() { return inputRails; }
    public void setInputRails(List<RailDefinition> inputRails) { this.inputRails = inputRails; }

    public List<RailDefinition> getOutputRails() { return outputRails; }
    public void setOutputRails(List<RailDefinition> outputRails) { this.outputRails = outputRails; }

    public AuditConfig getAudit() { return audit; }
    public void setAudit(AuditConfig audit) { this.audit = audit; }

    public MetricsConfig getMetrics() { return metrics; }
    public void setMetrics(MetricsConfig metrics) { this.metrics = metrics; }

    /** @return {@code true} if fail-open strategy is configured */
    public boolean isFailOpen() {
        return "open".equalsIgnoreCase(failStrategy);
    }

    // -------------------------------------------------------------------------
    // Nested config classes
    // -------------------------------------------------------------------------

    /** LLM-as-judge configuration. */
    public static class LlmJudgeConfig {
        private String provider = "openai";
        @JsonProperty("api-key")
        private String apiKey;
        private String model = "gpt-4o-mini";
        @JsonProperty("base-url")
        private String baseUrl = "https://api.openai.com/v1";
        @JsonProperty("timeout-ms")
        private int timeoutMs = 5000;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    /** Audit subsystem configuration. */
    public static class AuditConfig {
        private boolean enabled = true;
        @JsonProperty("log-level")
        private String logLevel = "INFO";
        @JsonProperty("include-original-text")
        private boolean includeOriginalText = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getLogLevel() { return logLevel; }
        public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
        public boolean isIncludeOriginalText() { return includeOriginalText; }
        public void setIncludeOriginalText(boolean includeOriginalText) {
            this.includeOriginalText = includeOriginalText;
        }
    }

    /** Metrics subsystem configuration. */
    public static class MetricsConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
