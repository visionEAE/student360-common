package co.edu.icesi.student360.common.api.exception;

import org.springframework.http.HttpStatus;

/** The resource genuinely does not exist. Never used to hide a resource the caller may not see. */
public class NotFoundException extends DomainException {

  public NotFoundException(String resourceType, String id) {
    super(HttpStatus.NOT_FOUND, "Not found", resourceType + " " + id + " does not exist");
  }
}
