package co.edu.icesi.student360.common.security.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.icesi.student360.common.security.InvalidServiceTokenException;
import co.edu.icesi.student360.common.security.ServiceIdentity;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The Google-token checks, against a locally generated RSA key standing in for Google's JWKS. */
class GoogleServiceTokenValidatorTest {

  static final String AUDIENCE = "https://s360-core-abc123.us-central1.run.app";
  static final String CALLER = "s360-run-gateway@project.iam.gserviceaccount.com";
  static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");
  static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private static RSAKey signingKey;
  private static RSAKey strangerKey;
  private static JWKSource<SecurityContext> keys;

  @BeforeAll
  static void generateKeys() throws JOSEException {
    signingKey = new RSAKeyGenerator(2048).keyID("google-1").generate();
    strangerKey = new RSAKeyGenerator(2048).keyID("google-1").generate();
    keys = new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK()));
  }

  @Test
  void shouldAcceptAWellFormedGoogleTokenAndExposeTheCallerEmail() throws JOSEException {
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(keys, AUDIENCE, List.of(), CLOCK);
    ServiceIdentity identity = validator.validate(token(signingKey, claims().build()));
    assertThat(identity.issuer()).isEqualTo(CALLER);
    assertThat(identity.audience()).isEqualTo(AUDIENCE);
  }

  @Test
  void shouldRejectTheWrongAudience() throws JOSEException {
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(keys, AUDIENCE, List.of(), CLOCK);
    String token = token(signingKey, claims().audience("https://someone-else.run.app").build());
    assertThatThrownBy(() -> validator.validate(token))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("not intended");
  }

  @Test
  void shouldRejectANonGoogleIssuer() throws JOSEException {
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(keys, AUDIENCE, List.of(), CLOCK);
    String token = token(signingKey, claims().issuer("https://evil.example").build());
    assertThatThrownBy(() -> validator.validate(token))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("not issued by Google");
  }

  @Test
  void shouldRejectAnExpiredToken() throws JOSEException {
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(keys, AUDIENCE, List.of(), CLOCK);
    String token =
        token(signingKey, claims().expirationTime(Date.from(NOW.minusSeconds(1))).build());
    assertThatThrownBy(() -> validator.validate(token))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("Expired");
  }

  @Test
  void shouldRejectASignatureFromAKeyGoogleNeverPublished() throws JOSEException {
    // Same kid, different key pair: the signature must fail, not silently pass on kid match.
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(keys, AUDIENCE, List.of(), CLOCK);
    String token = token(strangerKey, claims().build());
    assertThatThrownBy(() -> validator.validate(token))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("signature");
  }

  @Test
  void shouldRejectACallerOutsideTheAllowlist() throws JOSEException {
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(
            keys, AUDIENCE, List.of("someone-else@project.iam.gserviceaccount.com"), CLOCK);
    assertThatThrownBy(() -> validator.validate(token(signingKey, claims().build())))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("not allowed");
  }

  @Test
  void shouldAcceptAnAllowlistedCaller() throws JOSEException {
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(keys, AUDIENCE, List.of(CALLER), CLOCK);
    assertThat(validator.validate(token(signingKey, claims().build())).issuer()).isEqualTo(CALLER);
  }

  @Test
  void shouldRejectGarbageOutright() {
    GoogleServiceTokenValidator validator =
        new GoogleServiceTokenValidator(keys, AUDIENCE, List.of(), CLOCK);
    assertThatThrownBy(() -> validator.validate("not-even-a-jwt"))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("Malformed");
  }

  private static JWTClaimsSet.Builder claims() {
    return new JWTClaimsSet.Builder()
        .issuer("https://accounts.google.com")
        .audience(AUDIENCE)
        .subject("1122334455")
        .claim("email", CALLER)
        .expirationTime(Date.from(NOW.plusSeconds(3600)))
        .issueTime(Date.from(NOW));
  }

  private static String token(RSAKey key, JWTClaimsSet claims) throws JOSEException {
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }
}
