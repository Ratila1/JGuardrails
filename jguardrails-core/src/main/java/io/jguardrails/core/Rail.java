package io.jguardrails.core;

/**
 * Base marker interface for all guardrail rails.
 *
 * <p>A Rail is a unit of processing in the guardrail pipeline.
 * Each rail receives context and data, inspects them,
 * and returns a result: pass, block, or modify.</p>
 *
 * <p>Rails are applied in priority order (lower number = earlier execution).
 * They can be dynamically enabled or disabled without removing them from the pipeline.</p>
 *
 * @see InputRail
 * @see OutputRail
 * @see RailResult
 */
public interface Rail {

    /**
     * Returns the unique name of this rail, used for logging and metrics.
     *
     * @return unique rail name
     */
    String name();

    /**
     * Returns the execution priority. Rails with lower numbers execute first.
     * Default priority is 100.
     *
     * @return priority value (lower = earlier)
     */
    default int priority() {
        return 100;
    }

    /**
     * Returns whether this rail is enabled.
     * Disabled rails are skipped during pipeline execution.
     *
     * @return {@code true} if the rail should execute, {@code false} to skip
     */
    default boolean isEnabled() {
        return true;
    }
}
