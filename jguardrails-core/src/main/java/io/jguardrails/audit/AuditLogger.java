package io.jguardrails.audit;

/**
 * Interface for logging guardrail audit events.
 *
 * <p>Implementations may write to SLF4J, a database, an external SIEM system,
 * or an in-memory list (for testing).</p>
 *
 * @see DefaultAuditLogger
 * @see InMemoryAuditLogger
 */
public interface AuditLogger {

    /**
     * Records an audit event.
     *
     * @param entry the audit entry to record
     */
    void log(AuditEntry entry);
}
