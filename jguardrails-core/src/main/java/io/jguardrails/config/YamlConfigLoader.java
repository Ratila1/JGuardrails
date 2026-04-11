package io.jguardrails.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.jguardrails.core.GuardrailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Loads {@link GuardrailConfig} from a YAML file or stream.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * GuardrailConfig config = YamlConfigLoader.load(Path.of("guardrails.yml"));
 * GuardrailConfig config = YamlConfigLoader.loadFromClasspath("guardrails.yml");
 * }</pre>
 */
public final class YamlConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(YamlConfigLoader.class);

    private static final ObjectMapper MAPPER = createMapper();

    private YamlConfigLoader() {}

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Loads configuration from a file path.
     *
     * @param path path to the YAML configuration file
     * @return parsed configuration
     * @throws GuardrailException if the file cannot be read or parsed
     */
    public static GuardrailConfig load(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        log.info("Loading JGuardrails configuration from: {}", path);
        try (InputStream is = Files.newInputStream(path)) {
            return loadFromStream(is);
        } catch (IOException e) {
            throw new GuardrailException("Failed to read guardrails config from: " + path, e);
        }
    }

    /**
     * Loads configuration from the classpath.
     *
     * @param resourcePath classpath resource path (e.g., {@code "guardrails.yml"})
     * @return parsed configuration
     * @throws GuardrailException if the resource is not found or cannot be parsed
     */
    public static GuardrailConfig loadFromClasspath(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        log.info("Loading JGuardrails configuration from classpath: {}", resourcePath);
        String normalised = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        InputStream is = YamlConfigLoader.class.getResourceAsStream(normalised);
        if (is == null) {
            throw new GuardrailException("Classpath resource not found: " + resourcePath);
        }
        try (is) {
            return loadFromStream(is);
        } catch (IOException e) {
            throw new GuardrailException("Failed to read guardrails config from classpath: " + resourcePath, e);
        }
    }

    /**
     * Loads configuration from an input stream.
     *
     * @param inputStream YAML input stream
     * @return parsed configuration
     * @throws GuardrailException if the YAML is invalid
     */
    public static GuardrailConfig loadFromStream(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        try {
            Map<?, ?> root = MAPPER.readValue(inputStream, Map.class);
            Object jguardrailsNode = root.get("jguardrails");
            if (jguardrailsNode == null) {
                log.warn("No 'jguardrails' key found in YAML config; using defaults");
                return new GuardrailConfig();
            }
            String json = MAPPER.writeValueAsString(jguardrailsNode);
            return MAPPER.readValue(json, GuardrailConfig.class);
        } catch (IOException e) {
            throw new GuardrailException("Failed to parse guardrails YAML configuration", e);
        }
    }
}
