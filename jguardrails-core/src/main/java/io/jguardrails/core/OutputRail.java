package io.jguardrails.core;

/**
 * A rail applied to the LLM's response BEFORE it is returned to the user.
 *
 * <p>An OutputRail can:</p>
 * <ul>
 *   <li>PASS – allow the response through unchanged</li>
 *   <li>BLOCK – suppress the response (user receives a fallback message)</li>
 *   <li>MODIFY – transform the response (e.g., remove PII from LLM output)</li>
 * </ul>
 *
 * <p>Output rails are sorted by {@link Rail#priority()} before execution.
 * Processing stops as soon as any rail returns BLOCK.</p>
 *
 * @see InputRail
 * @see RailResult
 * @see RailContext
 */
public interface OutputRail extends Rail {

    /**
     * Processes the LLM's outgoing response.
     *
     * @param output        the LLM response text
     * @param originalInput the original user request (for context/relevance checks)
     * @param context       the pipeline execution context
     * @return a result indicating pass, block, or modify with updated text
     */
    RailResult process(String output, String originalInput, RailContext context);
}
