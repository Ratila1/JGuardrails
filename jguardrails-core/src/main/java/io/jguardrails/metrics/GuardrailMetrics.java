package io.jguardrails.metrics;

/**
 * Interface for collecting guardrail pipeline metrics.
 *
 * <p>Implementations may write to Micrometer, Prometheus, or an in-memory store.</p>
 *
 * @see DefaultMetrics
 */
public interface GuardrailMetrics {

    /**
     * Records that a rail blocked a request.
     *
     * @param railName the name of the blocking rail
     */
    void recordBlock(String railName);

    /**
     * Records that a rail modified the text.
     *
     * @param railName the name of the modifying rail
     */
    void recordModification(String railName);

    /**
     * Records that a rail (or the whole pipeline phase) passed without intervention.
     *
     * @param railName the name of the rail or pipeline phase
     */
    void recordPass(String railName);

    /**
     * Records that a rail threw an exception.
     *
     * @param railName the name of the failing rail
     */
    void recordError(String railName);
}
