package co.edu.icesi.student360.common.audit;

import co.edu.icesi.student360.common.identity.Identity;
import co.edu.icesi.student360.common.identity.IdentityContext;
import co.edu.icesi.student360.common.logging.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Application-facing entry point of the audit trail. Fills in everything that is ambient — when,
 * which request, which trace, which service, which actor, from where — so callers only state what
 * happened. Used by the {@link AuditAspect} and directly by services for security events.
 */
public class AuditTrail {

  /** Request id used for records written outside any HTTP request (scheduled work, startup). */
  static final String NO_REQUEST = "system";

  private final AuditWriter writer;
  private final String serviceName;
  private final Clock clock;

  public AuditTrail(AuditWriter writer, String serviceName, Clock clock) {
    this.writer = writer;
    this.serviceName = serviceName;
    this.clock = clock;
  }

  public void record(
      RecordType recordType,
      String action,
      String subjectType,
      String subjectId,
      AuthorizationBasis basis,
      Outcome outcome,
      Map<String, Object> details) {
    recordAs(
        IdentityContext.current().orElse(null),
        recordType,
        action,
        subjectType,
        subjectId,
        basis,
        outcome,
        details);
  }

  /**
   * Same as {@link #record}, with an explicit actor. Needed where no gateway identity exists yet
   * but the actor is known — the SSO recording who just logged in or whose session was revoked.
   */
  public void recordAs(
      Identity explicitActor,
      RecordType recordType,
      String action,
      String subjectType,
      String subjectId,
      AuthorizationBasis basis,
      Outcome outcome,
      Map<String, Object> details) {
    Optional<Identity> actor = Optional.ofNullable(explicitActor);
    AuditRecord record =
        AuditRecord.builder()
            .occurredAt(clock.instant())
            .requestId(Correlation.currentRequestId().orElse(NO_REQUEST))
            .traceId(Correlation.currentTraceId().orElse(null))
            .serviceName(serviceName)
            .recordType(recordType)
            .action(action)
            .actorId(actor.map(Identity::userId).orElse(null))
            .actorRoles(actor.map(Identity::roles).orElse(null))
            .subjectType(subjectType)
            .subjectId(subjectId)
            .authorizationBasis(basis)
            .outcome(outcome)
            .sourceIp(currentSourceIp().orElse(null))
            .details(details)
            .build();
    writer.write(record);
  }

  /** Prefers the address the gateway saw over the gateway's own address. */
  private static Optional<String> currentSourceIp() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
      return Optional.empty();
    }
    HttpServletRequest request = attrs.getRequest();
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return Optional.of(forwarded.split(",")[0].trim());
    }
    return Optional.ofNullable(request.getRemoteAddr());
  }
}
