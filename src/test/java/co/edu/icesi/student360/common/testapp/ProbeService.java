package co.edu.icesi.student360.common.testapp;

import co.edu.icesi.student360.common.api.exception.AccessDeniedForSubjectException;
import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.audit.AuthorizationBasis;
import co.edu.icesi.student360.common.audit.AuthorizationBasisHolder;
import co.edu.icesi.student360.common.identity.Identity;
import co.edu.icesi.student360.common.identity.IdentityContext;
import co.edu.icesi.student360.common.outbox.DomainEvent;
import co.edu.icesi.student360.common.outbox.EventPublisher;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProbeService {

  private final EventPublisher events;

  public ProbeService(EventPublisher events) {
    this.events = events;
  }

  /** The self policy from the domain services, in miniature. */
  @Audited(action = "READ_PROBE", subjectType = "STUDENT")
  @Transactional
  public Map<String, String> read(String studentId) {
    Identity caller = IdentityContext.require();
    if (!studentId.equals(caller.externalReference())) {
      throw new AccessDeniedForSubjectException("STUDENT", studentId);
    }
    AuthorizationBasisHolder.grant(AuthorizationBasis.SELF);
    events.publish(
        new DomainEvent("PROBE_READ", "STUDENT", studentId, Instant.now(), Map.of("ok", true)));
    return Map.of("studentId", studentId);
  }
}
