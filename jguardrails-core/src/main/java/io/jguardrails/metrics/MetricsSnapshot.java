package io.jguardrails.metrics;

import java.util.Map;

/**
 * An immutable snapshot of guardrail pipeline metrics.
 *
 * @see DefaultMetrics#getSnapshot()
 */
public record MetricsSnapshot(
        long totalBlocked,
        long totalModified,
        long totalPassed,
        long totalErrors,
        Map<String, Long> blockedByRail,
        Map<String, Long> modifiedByRail,
        Map<String, Long> errorsByRail
) {

    /**
     * Compact constructor ensuring map immutability.
     */
    public MetricsSnapshot {
        blockedByRail = Map.copyOf(blockedByRail);
        modifiedByRail = Map.copyOf(modifiedByRail);
        errorsByRail = Map.copyOf(errorsByRail);
    }
}
