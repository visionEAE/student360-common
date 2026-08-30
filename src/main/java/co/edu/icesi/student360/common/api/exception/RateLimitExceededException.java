package co.edu.icesi.student360.common.api.exception;

import java.time.Duration;
import org.springframework.http.HttpStatus;

/** Too many attempts from the same origin; the API answers 429 with a Retry-After header. */
public class RateLimitExceededException extends DomainException {

  private final Duration retryAfter;

  public RateLimitExceededException(Duration retryAfter) {
    super(HttpStatus.TOO_MANY_REQUESTS, "Too many requests", "Retry later");
    this.retryAfter = retryAfter;
  }

  public Duration retryAfter() {
    return retryAfter;
  }
}
