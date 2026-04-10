package io.jguardrails.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J-backed audit logger.
 *
 * <p>Logs BLOCKED events at WARN level and MODIFIED events at INFO level.</p>
 */
public class DefaultAuditLogger implements AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("io.jguardrails.audit");

    @Override
    public void log(AuditEntry entry) {
        if (entry.getType() == AuditEntry.Type.BLOCKED) {
            log.warn("[GUARDRAIL AUDIT] BLOCKED by rail='{}' reason='{}' at {}",
                    entry.getRailName(), entry.getReason(), entry.getTimestamp());
        } else {
            log.info("[GUARDRAIL AUDIT] MODIFIED by rail='{}' reason='{}' at {}",
                    entry.getRailName(), entry.getReason(), entry.getTimestamp());
        }
    }
}
