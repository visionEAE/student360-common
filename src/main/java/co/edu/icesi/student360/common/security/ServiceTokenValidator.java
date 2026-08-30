package co.edu.icesi.student360.common.security;

/** Port: verifies an inbound service token and extracts who issued it and for whom. */
public interface ServiceTokenValidator {

  /**
   * Validates signature, expiry and that the audience is this service.
   *
   * @throws InvalidServiceTokenException when any check fails; the message is safe to expose
   */
  ServiceIdentity validate(String token);
}
