package co.edu.icesi.student360.common.audit;

import co.edu.icesi.student360.common.api.exception.AccessDeniedForSubjectException;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Writes one record per {@link Audited} invocation. A normal return is ALLOWED with the basis the
 * access policy declared; an {@link AccessDeniedForSubjectException} is DENIED with basis NONE and
 * is rethrown untouched. Other exceptions are not the trail's business and pass through.
 */
@Aspect
public class AuditAspect {

  private final AuditTrail trail;

  public AuditAspect(AuditTrail trail) {
    this.trail = trail;
  }

  @Around("@annotation(audited)")
  public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
    String subjectId = resolveSubjectId(joinPoint, audited);
    AuthorizationBasisHolder.clear();
    try {
      Object result = joinPoint.proceed();
      trail.record(
          audited.recordType(),
          audited.action(),
          audited.subjectType(),
          subjectId,
          AuthorizationBasisHolder.current().orElse(null),
          Outcome.ALLOWED,
          Map.of());
      return result;
    } catch (AccessDeniedForSubjectException denied) {
      trail.record(
          audited.recordType(),
          audited.action(),
          audited.subjectType(),
          subjectId,
          AuthorizationBasis.NONE,
          Outcome.DENIED,
          Map.of("reason", denied.getMessage()));
      throw denied;
    } finally {
      AuthorizationBasisHolder.clear();
    }
  }

  private static String resolveSubjectId(ProceedingJoinPoint joinPoint, Audited audited) {
    Object[] arguments = joinPoint.getArgs();
    if (arguments.length == 0) {
      return null;
    }
    if (audited.subjectIdParameter().isEmpty()) {
      return String.valueOf(arguments[0]);
    }
    String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
    for (int index = 0; names != null && index < names.length; index++) {
      if (audited.subjectIdParameter().equals(names[index])) {
        return String.valueOf(arguments[index]);
      }
    }
    throw new IllegalStateException(
        "No parameter named " + audited.subjectIdParameter() + " on " + joinPoint.getSignature());
  }
}
