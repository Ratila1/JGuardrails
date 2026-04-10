package io.jguardrails.spring;

import io.jguardrails.audit.DefaultAuditLogger;
import io.jguardrails.config.GuardrailConfig;
import io.jguardrails.config.RailDefinition;
import io.jguardrails.config.YamlConfigLoader;
import io.jguardrails.detectors.input.jailbreak.JailbreakDetector;
import io.jguardrails.detectors.input.length.InputLengthValidator;
import io.jguardrails.detectors.input.pii.PiiMasker;
import io.jguardrails.detectors.output.length.OutputLengthValidator;
import io.jguardrails.detectors.output.toxicity.ToxicityChecker;
import io.jguardrails.pipeline.GuardrailPipeline;
import io.jguardrails.pipeline.PipelineBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Spring Boot auto-configuration for JGuardrails.
 *
 * <p>Automatically creates and registers a {@link GuardrailPipeline} bean
 * and a {@link GuardrailAdvisor} when {@code jguardrails.enabled=true} (default).</p>
 *
 * <p>Configure via {@code application.yml}:</p>
 * <pre>{@code
 * jguardrails:
 *   enabled: true
 *   config-path: classpath:guardrails.yml
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "jguardrails", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GuardrailProperties.class)
public class GuardrailAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GuardrailAutoConfiguration.class);

    /**
     * Creates the default {@link GuardrailPipeline} bean from the YAML configuration.
     *
     * @param properties JGuardrails Spring properties
     * @return configured pipeline
     */
    @Bean
    @ConditionalOnMissingBean
    public GuardrailPipeline guardrailPipeline(GuardrailProperties properties) {
        log.info("Initialising JGuardrails pipeline from config: {}", properties.getConfigPath());

        GuardrailConfig config = loadConfig(properties.getConfigPath());

        PipelineBuilder builder = GuardrailPipeline.builder()
                .failOpen(config.isFailOpen())
                .blockedResponse(config.getBlockedResponse())
                .auditLogger(new DefaultAuditLogger());

        for (RailDefinition railDef : config.getInputRails()) {
            if (!railDef.isEnabled()) continue;
            switch (railDef.getType()) {
                case "jailbreak-detect" -> builder.addInputRail(
                    JailbreakDetector.builder().priority(railDef.getPriority()).build()
                );
                case "pii-mask" -> builder.addInputRail(
                    PiiMasker.builder().priority(railDef.getPriority()).build()
                );
                case "input-length" -> builder.addInputRail(
                    InputLengthValidator.builder()
                        .priority(railDef.getPriority())
                        .maxCharacters(railDef.getInt("max-characters", 10000))
                        .build()
                );
                default -> log.warn("Unknown input rail type: {}", railDef.getType());
            }
        }

        for (RailDefinition railDef : config.getOutputRails()) {
            if (!railDef.isEnabled()) continue;
            switch (railDef.getType()) {
                case "toxicity-check" -> builder.addOutputRail(
                    ToxicityChecker.builder().priority(railDef.getPriority()).build()
                );
                case "output-length" -> builder.addOutputRail(
                    OutputLengthValidator.builder()
                        .priority(railDef.getPriority())
                        .maxCharacters(railDef.getInt("max-characters", 5000))
                        .truncate(railDef.getBoolean("truncate", false))
                        .build()
                );
                default -> log.warn("Unknown output rail type: {}", railDef.getType());
            }
        }

        return builder.build();
    }

    /**
     * Creates the default {@link GuardrailAdvisor} bean.
     *
     * @param pipeline the guardrail pipeline
     * @return the advisor
     */
    @Bean
    @ConditionalOnMissingBean
    public GuardrailAdvisor guardrailAdvisor(GuardrailPipeline pipeline) {
        return new GuardrailAdvisor(pipeline);
    }

    private GuardrailConfig loadConfig(String configPath) {
        try {
            Resource resource;
            if (configPath.startsWith("classpath:")) {
                resource = new ClassPathResource(configPath.substring("classpath:".length()));
            } else if (configPath.startsWith("file:")) {
                resource = new FileSystemResource(configPath.substring("file:".length()));
            } else {
                resource = new ClassPathResource(configPath);
            }

            if (!resource.exists()) {
                log.warn("JGuardrails config not found at '{}'; using defaults", configPath);
                return new GuardrailConfig();
            }

            try (InputStream is = resource.getInputStream()) {
                return YamlConfigLoader.loadFromStream(is);
            }
        } catch (IOException e) {
            log.warn("Failed to load JGuardrails config from '{}'; using defaults", configPath, e);
            return new GuardrailConfig();
        }
    }
}
