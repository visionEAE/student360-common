package co.edu.icesi.student360.common.authorization;

import co.edu.icesi.student360.common.api.exception.AccessDeniedForSubjectException;
import co.edu.icesi.student360.common.audit.AuthorizationBasis;
import co.edu.icesi.student360.common.audit.AuthorizationBasisHolder;
import co.edu.icesi.student360.common.identity.Identity;
import co.edu.icesi.student360.common.identity.IdentityContext;

/**
 * Fine-grained authorization for a student's institutional records (core-service, lms-service). A
 * student may read only themself — the {@code ref} claim must equal the requested id; advisors and
 * admins may read any student. The basis is recorded for the audit aspect, so the trail says not
 * only that access happened but why it was allowed.
 *
 * <p>Call it inside the {@code @Audited} method, so a denial is recorded as DENIED.
 */
public class StudentRecordAccessPolicy {

  public static final String STUDENT = "STUDENT";
  public static final String ADVISOR = "ADVISOR";
  public static final String ADMIN = "ADMIN";
  public static final String SUBJECT_TYPE = "STUDENT";

  public void assertCanRead(String studentId) {
    Identity caller = IdentityContext.require();
    if (caller.hasRole(ADMIN)) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.ADMIN_ROLE);
      return;
    }
    if (caller.hasRole(ADVISOR)) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.STAFF_ROLE);
      return;
    }
    if (caller.hasRole(STUDENT) && studentId.equals(caller.externalReference())) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.SELF);
      return;
    }
    throw new AccessDeniedForSubjectException(SUBJECT_TYPE, studentId);
  }
}
