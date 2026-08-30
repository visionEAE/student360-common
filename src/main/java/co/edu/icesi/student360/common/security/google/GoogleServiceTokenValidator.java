package co.edu.icesi.student360.common.security.google;

import co.edu.icesi.student360.common.security.InvalidServiceTokenException;
import co.edu.icesi.student360.common.security.ServiceIdentity;
import co.edu.icesi.student360.common.security.ServiceTokenValidator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Stage 2 counterpart of {@link GoogleServiceTokenProvider}: validates a Google-signed ID token
 * whose audience is this service's own URL. On a private Cloud Run service the platform has already
 * checked the same header before the request reached the container — this validation is deliberate
 * defense in depth, and it is also what turns the token into a {@link ServiceIdentity} (the
 * caller's service-account email) for the identity-header trust chain downstream.
 *
 * <p>{@code allowedCallers} narrows acceptance to specific service accounts; empty means any
 * Google-signed token with the right audience, which Cloud Run IAM already restricts to the
 * accounts granted {@code run.invoker}.
 */
public class GoogleServiceTokenValidator implements ServiceTokenValidator {

  static final Set<String> GOOGLE_ISSUERS =
      Set.of("accounts.google.com", "https://accounts.google.com");

  private final JWKSource<SecurityContext> keys;
  private final String expectedAudience;
  private final List<String> allowedCallers;
  private final Clock clock;

  public GoogleServiceTokenValidator(
      JWKSource<SecurityContext> keys,
      String expectedAudience,
      List<String> allowedCallers,
      Clock clock) {
    this.keys = keys;
    this.expectedAudience = expectedAudience;
    this.allowedCallers = List.copyOf(allowedCallers);
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
    if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
      throw new InvalidServiceTokenException("Unexpected service token algorithm");
    }
    verifySignature(jwt);
    if (claims.getIssuer() == null || !GOOGLE_ISSUERS.contains(claims.getIssuer())) {
      throw new InvalidServiceTokenException("Service token not issued by Google");
    }
    Date expiration = claims.getExpirationTime();
    if (expiration == null || !expiration.toInstant().isAfter(clock.instant())) {
      throw new InvalidServiceTokenException("Expired service token");
    }
    if (claims.getAudience() == null || !claims.getAudience().contains(expectedAudience)) {
      throw new InvalidServiceTokenException("Service token not intended for this service");
    }
    String caller = stringClaim(claims, "email");
    if (!allowedCallers.isEmpty() && (caller == null || !allowedCallers.contains(caller))) {
      throw new InvalidServiceTokenException("Caller is not allowed to invoke this service");
    }
    return new ServiceIdentity(caller == null ? claims.getIssuer() : caller, expectedAudience);
  }

  private void verifySignature(SignedJWT jwt) {
    List<JWK> candidates;
    try {
      candidates = keys.get(new JWKSelector(JWKMatcher.forJWSHeader(jwt.getHeader())), null);
    } catch (Exception exception) {
      throw new InvalidServiceTokenException("Could not load Google signing keys");
    }
    for (JWK candidate : candidates) {
      try {
        if (candidate instanceof RSAKey rsaKey
            && jwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()))) {
          return;
        }
      } catch (JOSEException exception) {
        // try the next candidate key
      }
    }
    throw new InvalidServiceTokenException("Invalid service token signature");
  }

  private static String stringClaim(JWTClaimsSet claims, String name) {
    try {
      return claims.getStringClaim(name);
    } catch (ParseException exception) {
      return null;
    }
  }
}
