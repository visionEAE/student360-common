package co.edu.icesi.student360.common.security;

import co.edu.icesi.student360.common.api.exception.AuthenticationFailedException;

/** The inbound service token is missing, malformed, expired, mis-signed or for another audience. */
public class InvalidServiceTokenException extends AuthenticationFailedException {

  public InvalidServiceTokenException(String detail) {
    super(detail);
  }
}
