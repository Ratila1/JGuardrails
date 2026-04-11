package io.jguardrails.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple thread-safe in-memory metrics implementation.
 *
 * <p>Suitable for development and low-volume production use.
 * For production use with Prometheus or Micrometer, implement {@link GuardrailMetrics}
 * and register it via {@link io.jguardrails.pipeline.PipelineBuilder#metrics(GuardrailMetrics)}.</p>
 */
public class DefaultMetrics implements GuardrailMetrics {

    private final LongAdder totalBlocked = new LongAdder();
    private final LongAdder totalModified = new LongAdder();
    private final LongAdder totalPassed = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();

    private final ConcurrentHashMap<String, LongAdder> blockedByRail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> modifiedByRail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> errorsByRail = new ConcurrentHashMap<>();

    @Override
    public void recordBlock(String railName) {
        totalBlocked.increment();
        blockedByRail.computeIfAbsent(railName, k -> new LongAdder()).increment();
    }

    @Override
    public void recordModification(String railName) {
        totalModified.increment();
        modifiedByRail.computeIfAbsent(railName, k -> new LongAdder()).increment();
    }

    @Override
    public void recordPass(String railName) {
        totalPassed.increment();
    }

    @Override
    public void recordError(String railName) {
        totalErrors.increment();
        errorsByRail.computeIfAbsent(railName, k -> new LongAdder()).increment();
    }

    /**
     * Returns an immutable snapshot of current metrics.
     *
     * @return metrics snapshot
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
                totalBlocked.sum(),
                totalModified.sum(),
                totalPassed.sum(),
                totalErrors.sum(),
                toMap(blockedByRail),
                toMap(modifiedByRail),
                toMap(errorsByRail)
        );
    }

    /**
     * Resets all counters to zero.
     */
    public void reset() {
        totalBlocked.reset();
        totalModified.reset();
        totalPassed.reset();
        totalErrors.reset();
        blockedByRail.clear();
        modifiedByRail.clear();
        errorsByRail.clear();
    }

    private Map<String, Long> toMap(ConcurrentHashMap<String, LongAdder> source) {
        Map<String, Long> result = new java.util.HashMap<>();
        source.forEach((k, v) -> result.put(k, v.sum()));
        return result;
    }
}
