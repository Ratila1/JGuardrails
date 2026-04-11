package io.jguardrails.core;

/**
 * A rail applied to the user's incoming message BEFORE it is sent to the LLM.
 *
 * <p>An InputRail can:</p>
 * <ul>
 *   <li>PASS – allow the message through unchanged</li>
 *   <li>BLOCK – prevent the message from reaching the LLM</li>
 *   <li>MODIFY – transform the message (e.g., mask PII) before forwarding</li>
 * </ul>
 *
 * <p>Input rails are sorted by {@link Rail#priority()} before execution.
 * Processing stops as soon as any rail returns BLOCK.</p>
 *
 * @see OutputRail
 * @see RailResult
 * @see RailContext
 */
public interface InputRail extends Rail {

    /**
     * Processes the incoming user message.
     *
     * @param input   the user's message text
     * @param context the pipeline execution context (history, metadata, settings)
     * @return a result indicating pass, block, or modify with updated text
     */
    RailResult process(String input, RailContext context);
}
