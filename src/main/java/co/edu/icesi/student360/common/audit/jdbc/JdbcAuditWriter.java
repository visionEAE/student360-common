package co.edu.icesi.student360.common.audit.jdbc;

import co.edu.icesi.student360.common.audit.AuditRecord;
import co.edu.icesi.student360.common.audit.AuditWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stage 1 adapter: a single INSERT into {@code audit.audit_record}. Runs in its own transaction so
 * that a denied access — whose business transaction is rolled back — still leaves its record. The
 * database role only holds INSERT and SELECT on the table, so this class could not do more even if
 * it wanted to.
 */
public class JdbcAuditWriter implements AuditWriter {

  private static final String INSERT =
      """
      INSERT INTO audit.audit_record (
          occurred_at, request_id, trace_id, service_name, record_type, action,
          actor_id, actor_roles, subject_type, subject_id, authorization_basis, outcome,
          source_ip, details)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper;

  public JdbcAuditWriter(
      JdbcTemplate jdbcTemplate,
      PlatformTransactionManager transactionManager,
      ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.objectMapper = objectMapper;
  }

  @Override
  public void write(AuditRecord record) {
    String details = serialize(record);
    transactionTemplate.executeWithoutResult(
        status ->
            jdbcTemplate.update(
                INSERT,
                Timestamp.from(record.occurredAt()),
                record.requestId(),
                record.traceId(),
                record.serviceName(),
                record.recordType().name(),
                record.action(),
                record.actorId(),
                record.actorRoles().toArray(String[]::new),
                record.subjectType(),
                record.subjectId(),
                Optional.ofNullable(record.authorizationBasis()).map(Enum::name).orElse(null),
                record.outcome().name(),
                record.sourceIp(),
                details));
  }

  private String serialize(AuditRecord record) {
    try {
      return objectMapper.writeValueAsString(record.details());
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Audit details are not serialisable", exception);
    }
  }
}
