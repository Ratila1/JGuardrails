package io.jguardrails.llm;

import java.util.concurrent.CompletableFuture;

/**
 * Minimal interface for invoking a language model.
 *
 * <p>Used exclusively for <em>LLM-as-judge</em> functionality within rails
 * (e.g., jailbreak detection, toxicity checking via LLM).
 * It is <strong>not</strong> used for the main application LLM call.</p>
 *
 * @see OpenAiClient
 * @see OllamaClient
 */
public interface LlmClient {

    /**
     * Sends a request to the LLM and returns its response synchronously.
     *
     * @param request the LLM request
     * @return the LLM response
     */
    LlmResponse chat(LlmRequest request);

    /**
     * Sends a request to the LLM and returns a future for the response.
     *
     * @param request the LLM request
     * @return a future that completes with the LLM response
     */
    CompletableFuture<LlmResponse> chatAsync(LlmRequest request);
}
