package io.jguardrails.detectors.input.pii;

import io.jguardrails.core.InputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks Personally Identifiable Information (PII) in user input before forwarding to the LLM.
 *
 * <p>Supports multiple {@link PiiEntity} types and configurable {@link PiiMaskingStrategy}:</p>
 * <ul>
 *   <li>{@link PiiMaskingStrategy#REDACT} — replaces with {@code [TYPE REDACTED]}</li>
 *   <li>{@link PiiMaskingStrategy#MASK_PARTIAL} — partially hides the value</li>
 *   <li>{@link PiiMaskingStrategy#HASH} — replaces with SHA-256 hex hash</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PiiMasker masker = PiiMasker.builder()
 *     .entities(PiiEntity.EMAIL, PiiEntity.PHONE, PiiEntity.CREDIT_CARD)
 *     .strategy(PiiMaskingStrategy.REDACT)
 *     .build();
 * }</pre>
 */
public class PiiMasker implements InputRail {

    private static final Logger log = LoggerFactory.getLogger(PiiMasker.class);

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final Set<PiiEntity> entities;
    private final PiiMaskingStrategy strategy;

    private PiiMasker(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.entities = EnumSet.copyOf(builder.entities.isEmpty()
                ? EnumSet.allOf(PiiEntity.class) : builder.entities);
        this.strategy = builder.strategy;
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

        String currentText = input;
        List<Map<String, String>> maskedEntities = new ArrayList<>();

        for (PiiEntity entity : entities) {
            Pattern pattern = PiiPattern.forEntity(entity);
            Matcher matcher = pattern.matcher(currentText);
            StringBuffer sb = new StringBuffer();
            boolean found = false;

            while (matcher.find()) {
                found = true;
                String original = matcher.group();
                String masked = maskValue(original, entity);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));

                Map<String, String> entityInfo = new HashMap<>();
                entityInfo.put("type", entity.name());
                entityInfo.put("masked", masked);
                maskedEntities.add(entityInfo);

                log.debug("Masked PII entity {} in input", entity);
            }

            if (found) {
                matcher.appendTail(sb);
                currentText = sb.toString();
            }
        }

        if (maskedEntities.isEmpty()) {
            return RailResult.pass(input, name());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("maskedEntities", maskedEntities);
        metadata.put("maskedCount", maskedEntities.size());

        return RailResult.modify(
                currentText,
                name(),
                "Masked " + maskedEntities.size() + " PII entities",
                metadata
        );
    }

    private String maskValue(String value, PiiEntity entity) {
        return switch (strategy) {
            case REDACT -> "[" + entity.name() + " REDACTED]";
            case MASK_PARTIAL -> maskPartial(value, entity);
            case HASH -> hashValue(value, entity);
        };
    }

    private String maskPartial(String value, PiiEntity entity) {
        return switch (entity) {
            case EMAIL -> {
                int at = value.indexOf('@');
                if (at <= 0) yield "[EMAIL REDACTED]";
                String local = value.substring(0, at);
                String domain = value.substring(at + 1);
                int dot = domain.lastIndexOf('.');
                String maskedLocal = local.charAt(0) + "***";
                String maskedDomain = dot > 0
                        ? domain.charAt(0) + "***" + domain.substring(dot)
                        : "***";
                yield maskedLocal + "@" + maskedDomain;
            }
            case PHONE -> {
                String digits = value.replaceAll("[^0-9+]", "");
                if (digits.length() <= 4) yield "****";
                yield digits.substring(0, digits.startsWith("+") ? 3 : 1) +
                      "***" + digits.substring(digits.length() - 4);
            }
            case CREDIT_CARD -> {
                String digits = value.replaceAll("[^0-9]", "");
                if (digits.length() < 4) yield "****";
                yield "****-****-****-" + digits.substring(digits.length() - 4);
            }
            default -> "[" + entity.name() + " REDACTED]";
        };
    }

    private String hashValue(String value, PiiEntity entity) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return "[" + entity.name() + ":" + HexFormat.of().formatHex(hash).substring(0, 12) + "]";
        } catch (NoSuchAlgorithmException e) {
            return "[" + entity.name() + " REDACTED]";
        }
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link PiiMasker}. */
    public static final class Builder {
        private String name = "pii-masker";
        private boolean enabled = true;
        private int priority = 20;
        private final Set<PiiEntity> entities = EnumSet.noneOf(PiiEntity.class);
        private PiiMaskingStrategy strategy = PiiMaskingStrategy.REDACT;

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder strategy(PiiMaskingStrategy strategy) { this.strategy = strategy; return this; }

        public Builder entities(PiiEntity... entities) {
            this.entities.addAll(Arrays.asList(entities));
            return this;
        }

        public Builder entities(Set<PiiEntity> entities) {
            this.entities.addAll(entities);
            return this;
        }

        public PiiMasker build() {
            return new PiiMasker(this);
        }
    }
}
