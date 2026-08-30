package co.edu.icesi.student360.common.outbox;

import co.edu.icesi.student360.common.logging.Correlation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Stage 1 adapter. The row is written in the caller's transaction on purpose: the outbox pattern
 * exists so that the business change and its event are committed or rolled back together. The
 * stored payload is the complete message envelope a Pub/Sub subscriber would receive.
 */
public class OutboxEventPublisher implements EventPublisher {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final String insertStatement;

  public OutboxEventPublisher(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock, String tableName) {
    if (!tableName.matches("[A-Za-z_][A-Za-z0-9_.]*")) {
      throw new IllegalArgumentException("Invalid outbox table name: " + tableName);
    }
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.insertStatement =
        "INSERT INTO "
            + tableName
            + " (id, event_type, aggregate_type, aggregate_id, payload, created_at)"
            + " VALUES (?, ?, ?, ?, ?::jsonb, ?)";
  }

  @Override
  public void publish(DomainEvent event) {
    UUID eventId = UUID.randomUUID();
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("eventId", eventId.toString());
    envelope.put("eventType", event.eventType());
    envelope.put("aggregateType", event.aggregateType());
    envelope.put("aggregateId", event.aggregateId());
    envelope.put("occurredAt", event.occurredAt().toString());
    envelope.put("requestId", Correlation.currentRequestId().orElse(null));
    envelope.put("traceId", Correlation.currentTraceId().orElse(null));
    envelope.put("data", event.data());
    jdbcTemplate.update(
        insertStatement,
        eventId,
        event.eventType(),
        event.aggregateType(),
        event.aggregateId(),
        serialize(envelope),
        Timestamp.from(clock.instant()));
  }

  private String serialize(Map<String, Object> envelope) {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Event payload is not serialisable", exception);
    }
  }
}
