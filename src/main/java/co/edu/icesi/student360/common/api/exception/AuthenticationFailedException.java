package co.edu.icesi.student360.common.api.exception;

import org.springframework.http.HttpStatus;

/** Missing or invalid authentication. The detail never reveals which part failed. */
public class AuthenticationFailedException extends DomainException {

  public AuthenticationFailedException(String detail) {
    super(HttpStatus.UNAUTHORIZED, "Authentication failed", detail);
  }
}
