package co.edu.icesi.student360.common.api.exception;

import org.springframework.http.HttpStatus;

/**
 * The caller is authenticated but holds no authorization relationship with the subject (a student,
 * an alert). Raised by fine-grained access policies; the audit aspect records it as a DENIED
 * outcome before it reaches the API as a 403.
 */
public class AccessDeniedForSubjectException extends DomainException {

  private final String subjectType;
  private final String subjectId;

  public AccessDeniedForSubjectException(String subjectType, String subjectId) {
    super(
        HttpStatus.FORBIDDEN,
        "Access denied",
        "No authorization relationship with " + subjectType.toLowerCase() + " " + subjectId);
    this.subjectType = subjectType;
    this.subjectId = subjectId;
  }

  public String subjectType() {
    return subjectType;
  }

  public String subjectId() {
    return subjectId;
  }
}
