package co.edu.icesi.student360.common.security.local;

import co.edu.icesi.student360.common.security.InvalidServiceTokenException;
import co.edu.icesi.student360.common.security.ServiceIdentity;
import co.edu.icesi.student360.common.security.ServiceTokenValidator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;

/** Stage 1 counterpart of {@link LocalServiceTokenProvider}: same secret, audience must be us. */
public class LocalServiceTokenValidator implements ServiceTokenValidator {

  private final String expectedAudience;
  private final byte[] secret;
  private final Clock clock;

  public LocalServiceTokenValidator(String expectedAudience, byte[] secret, Clock clock) {
    this.expectedAudience = expectedAudience;
    this.secret = secret.clone();
    this.clock = clock;
  }

  @Override
  public ServiceIdentity validate(String token) {
    SignedJWT jwt;
    JWTClaimsSet claims;
    try {
      jwt = SignedJWT.parse(token);
      claims = jwt.getJWTClaimsSet();
    } catch (ParseException exception) {
      throw new InvalidServiceTokenException("Malformed service token");
    }
    if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
      throw new InvalidServiceTokenException("Unexpected service token algorithm");
    }
    try {
      if (!jwt.verify(new MACVerifier(secret))) {
        throw new InvalidServiceTokenException("Invalid service token signature");
      }
    } catch (JOSEException exception) {
      throw new InvalidServiceTokenException("Invalid service token signature");
    }
    Date expiration = claims.getExpirationTime();
    if (expiration == null || !expiration.toInstant().isAfter(clock.instant())) {
      throw new InvalidServiceTokenException("Expired service token");
    }
    if (claims.getAudience() == null || !claims.getAudience().contains(expectedAudience)) {
      throw new InvalidServiceTokenException("Service token not intended for this service");
    }
    if (claims.getIssuer() == null || claims.getIssuer().isBlank()) {
      throw new InvalidServiceTokenException("Service token without issuer");
    }
    return new ServiceIdentity(claims.getIssuer(), expectedAudience);
  }
}
