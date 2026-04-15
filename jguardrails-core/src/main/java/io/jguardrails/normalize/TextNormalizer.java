package io.jguardrails.normalize;

/**
 * Converts raw input text into a canonical form that is easier for detectors to match against.
 *
 * <p>A normalizer is applied once per pipeline invocation, before any rails run.
 * The result is stored in the {@link io.jguardrails.core.RailContext} under the key
 * {@link #CONTEXT_KEY} and made available to detectors that benefit from canonical text
 * (jailbreak, topic, toxicity).</p>
 *
 * <p><strong>PII masking always operates on the original text</strong> to preserve entity
 * formatting; the normalized form is used only for detection.</p>
 *
 * <p>Implement this interface to plug in a custom normalization strategy:</p>
 * <pre>{@code
 * GuardrailPipeline pipeline = GuardrailPipeline.builder()
 *     .normalizer(myCustomNormalizer)
 *     .addInputRail(new JailbreakDetector())
 *     .build();
 * }</pre>
 *
 * @see DefaultTextNormalizer
 */
public interface TextNormalizer {

    /**
     * The {@link io.jguardrails.core.RailContext} attribute key under which the
     * normalized text is stored by the pipeline before running rails.
     *
     * <p>Rails can read it via:</p>
     * <pre>{@code
     * String text = context.getAttribute(TextNormalizer.CONTEXT_KEY, String.class)
     *                       .orElse(input);
     * }</pre>
     */
    String CONTEXT_KEY = "jg.normalizedInput";

    /**
     * Normalizes the given text into a canonical form.
     *
     * @param text raw input text; never {@code null}
     * @return normalized text; never {@code null}
     */
    String normalize(String text);
}
