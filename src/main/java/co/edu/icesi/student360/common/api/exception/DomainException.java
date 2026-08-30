package co.edu.icesi.student360.common.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Base of every exception a domain service may raise towards the API layer. Carrying the HTTP
 * status here keeps controllers free of translation code and guarantees one consistent mapping.
 */
public abstract class DomainException extends RuntimeException {

  private final HttpStatus status;
  private final String title;

  protected DomainException(HttpStatus status, String title, String detail) {
    super(detail);
    this.status = status;
    this.title = title;
  }

  public HttpStatus status() {
    return status;
  }

  public String title() {
    return title;
  }
}
