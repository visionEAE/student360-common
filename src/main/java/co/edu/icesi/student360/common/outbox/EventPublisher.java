package co.edu.icesi.student360.common.outbox;

/**
 * Port: hands a domain event to whoever delivers it. Stage 1 writes it to the service's outbox
 * table in the caller's transaction; stage 2 adds a relay that drains the outbox into Pub/Sub. The
 * domain service calls the same method either way.
 */
public interface EventPublisher {

  void publish(DomainEvent event);
}
