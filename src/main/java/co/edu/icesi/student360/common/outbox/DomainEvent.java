package co.edu.icesi.student360.common.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Something that happened in the domain and that other systems may react to. {@code aggregateId}
 * becomes the Pub/Sub ordering key in stage 2, so events about one student stay in order.
 */
public record DomainEvent(
    String eventType,
    String aggregateType,
    String aggregateId,
    Instant occurredAt,
    Map<String, Object> data) {

  public DomainEvent {
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(aggregateType, "aggregateType");
    Objects.requireNonNull(aggregateId, "aggregateId");
    Objects.requireNonNull(occurredAt, "occurredAt");
    data = data == null ? Map.of() : Map.copyOf(data);
  }
}
