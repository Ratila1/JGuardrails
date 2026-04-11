package io.jguardrails.config;

import io.jguardrails.core.GuardrailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("YamlConfigLoader")
class YamlConfigLoaderTest {

    private static final String VALID_YAML = """
            jguardrails:
              fail-strategy: open
              blocked-response: "Blocked!"
              input-rails:
                - type: jailbreak-detect
                  enabled: true
                  priority: 10
              output-rails:
                - type: toxicity-check
                  enabled: false
                  priority: 5
            """;

    @Test
    @DisplayName("parses valid YAML from stream")
    void parsesValidYaml() {
        GuardrailConfig config = YamlConfigLoader.loadFromStream(
                new ByteArrayInputStream(VALID_YAML.getBytes(StandardCharsets.UTF_8)));

        assertThat(config.isFailOpen()).isTrue();
        assertThat(config.getBlockedResponse()).isEqualTo("Blocked!");
        assertThat(config.getInputRails()).hasSize(1);
        assertThat(config.getInputRails().get(0).getType()).isEqualTo("jailbreak-detect");
        assertThat(config.getOutputRails()).hasSize(1);
        assertThat(config.getOutputRails().get(0).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("returns defaults when jguardrails key is missing")
    void returnsDefaults_whenKeyMissing() {
        String yaml = "other-config:\n  key: value\n";
        GuardrailConfig config = YamlConfigLoader.loadFromStream(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertThat(config.isFailOpen()).isFalse();
        assertThat(config.getInputRails()).isEmpty();
    }

    @Test
    @DisplayName("throws GuardrailException for invalid YAML")
    void throwsOnInvalidYaml() {
        String badYaml = "{ unclosed: [ brace";
        assertThatThrownBy(() -> YamlConfigLoader.loadFromStream(
                new ByteArrayInputStream(badYaml.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(GuardrailException.class);
    }

    @Test
    @DisplayName("throws GuardrailException when classpath resource not found")
    void throwsWhenClasspathResourceMissing() {
        assertThatThrownBy(() -> YamlConfigLoader.loadFromClasspath("nonexistent-file.yml"))
                .isInstanceOf(GuardrailException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("RailDefinition typed getters return correct values")
    void railDefinitionGetters() {
        String yaml = """
                jguardrails:
                  input-rails:
                    - type: input-length
                      config:
                        max-characters: 500
                        truncate: true
                """;
        GuardrailConfig config = YamlConfigLoader.loadFromStream(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        RailDefinition def = config.getInputRails().get(0);
        assertThat(def.getInt("max-characters", 0)).isEqualTo(500);
        assertThat(def.getBoolean("truncate", false)).isTrue();
        assertThat(def.getString("missing", "default")).isEqualTo("default");
    }
}
