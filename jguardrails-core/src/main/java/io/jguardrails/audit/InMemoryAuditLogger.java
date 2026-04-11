package io.jguardrails.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory audit logger for testing and development.
 *
 * <p>Stores all audit entries in a list that can be inspected in tests.</p>
 *
 * <p>Example usage in tests:</p>
 * <pre>{@code
 * InMemoryAuditLogger auditLogger = new InMemoryAuditLogger();
 * GuardrailPipeline pipeline = GuardrailPipeline.builder()
 *     .auditLogger(auditLogger)
 *     .build();
 *
 * // After processing...
 * assertThat(auditLogger.getEntries()).hasSize(1);
 * assertThat(auditLogger.getEntries().get(0).getType()).isEqualTo(AuditEntry.Type.BLOCKED);
 * }</pre>
 */
public class InMemoryAuditLogger implements AuditLogger {

    private final List<AuditEntry> entries = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void log(AuditEntry entry) {
        entries.add(entry);
    }

    /**
     * Returns an unmodifiable view of all recorded audit entries.
     *
     * @return list of audit entries in recording order
     */
    public List<AuditEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Returns only the entries of a given type.
     *
     * @param type entry type to filter by
     * @return filtered list
     */
    public List<AuditEntry> getEntries(AuditEntry.Type type) {
        return entries.stream()
                .filter(e -> e.getType() == type)
                .toList();
    }

    /**
     * Clears all recorded entries.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Returns the number of recorded entries.
     *
     * @return entry count
     */
    public int size() {
        return entries.size();
    }
}
