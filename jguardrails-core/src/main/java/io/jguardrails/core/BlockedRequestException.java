package io.jguardrails.core;

/**
 * Exception thrown when the guardrail pipeline blocks a request or response.
 *
 * <p>This exception is only thrown when the pipeline is configured to throw
 * on block (the default behaviour). If a blocked-response handler is provided,
 * the handler's return value is used instead.</p>
 *
 * @see io.jguardrails.pipeline.GuardrailPipeline
 */
public class BlockedRequestException extends GuardrailException {

    private final RailResult blockResult;

    /**
     * Constructs a blocked-request exception.
     *
     * @param blockResult the rail result that caused the block
     */
    public BlockedRequestException(RailResult blockResult) {
        super(String.format("Request blocked by rail '%s': %s",
                blockResult.railName(), blockResult.reason()));
        this.blockResult = blockResult;
    }

    /**
     * Returns the rail result that triggered the block.
     *
     * @return blocking rail result
     */
    public RailResult getBlockResult() {
        return blockResult;
    }
}
