package io.jguardrails.detectors.output.pii;

import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import io.jguardrails.detectors.input.pii.PiiEntity;
import io.jguardrails.detectors.input.pii.PiiMasker;
import io.jguardrails.detectors.input.pii.PiiMaskingStrategy;

import java.util.Objects;

/**
 * Scans LLM responses for PII and masks it before returning to the user.
 *
 * <p>Delegates detection and masking logic to {@link PiiMasker}, which is
 * also available as an {@link io.jguardrails.core.InputRail}.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * OutputPiiScanner scanner = OutputPiiScanner.builder()
 *     .entities(PiiEntity.EMAIL, PiiEntity.PHONE)
 *     .strategy(PiiMaskingStrategy.MASK_PARTIAL)
 *     .build();
 * }</pre>
 */
public class OutputPiiScanner implements OutputRail {

    private final String name;
    private final boolean enabled;
    private final int priority;
    private final PiiMasker delegate;

    private OutputPiiScanner(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.priority = builder.priority;
        this.delegate = builder.maskerBuilder.name(name + "-delegate").build();
    }

    @Override
    public String name() { return name; }

    @Override
    public int priority() { return priority; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public RailResult process(String output, String originalInput, RailContext context) {
        Objects.requireNonNull(output, "output must not be null");
        RailResult delegateResult = delegate.process(output, context);
        if (delegateResult.isModified()) {
            return RailResult.modify(delegateResult.text(), name(), delegateResult.reason(),
                    delegateResult.metadata());
        }
        return RailResult.pass(output, name());
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link OutputPiiScanner}. */
    public static final class Builder {
        private String name = "output-pii-scanner";
        private boolean enabled = true;
        private int priority = 20;
        private final PiiMasker.Builder maskerBuilder = PiiMasker.builder();

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }

        public Builder entities(PiiEntity... entities) {
            maskerBuilder.entities(entities);
            return this;
        }

        public Builder strategy(PiiMaskingStrategy strategy) {
            maskerBuilder.strategy(strategy);
            return this;
        }

        public OutputPiiScanner build() {
            return new OutputPiiScanner(this);
        }
    }
}
