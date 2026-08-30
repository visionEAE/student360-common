package co.edu.icesi.student360.common.api;

import co.edu.icesi.student360.common.api.exception.DomainException;
import co.edu.icesi.student360.common.api.exception.RateLimitExceededException;
import co.edu.icesi.student360.common.logging.Correlation;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * One RFC 7807 shape for every error of every service. Responses carry the request id so a user can
 * quote it, and never a stack trace, an SQL fragment or an internal class name.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
    ProblemDetail problem = problem(exception.status(), exception.title(), exception.getMessage());
    HttpHeaders headers = new HttpHeaders();
    if (exception instanceof RateLimitExceededException rateLimited) {
      headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimited.retryAfter().toSeconds()));
    }
    return ResponseEntity.status(exception.status()).headers(headers).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
    Map<String, String> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    error -> String.valueOf(error.getDefaultMessage()),
                    (first, second) -> first));
    ProblemDetail problem =
        problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
    log.error("Unhandled exception", exception);
    ProblemDetail problem =
        problem(
            HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", "An unexpected error occurred");
    return ResponseEntity.internalServerError().body(problem);
  }

  private static ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    Correlation.currentRequestId().ifPresent(id -> problem.setProperty("requestId", id));
    return problem;
  }
}
