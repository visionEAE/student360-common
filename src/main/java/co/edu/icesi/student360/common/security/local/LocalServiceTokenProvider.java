package co.edu.icesi.student360.common.security.local;

import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage 1 adapter: an HS256 JWT signed with a development secret shared by the services, with the
 * same claim structure a Google ID token carries ({@code iss}, {@code aud}, {@code iat}, {@code
 * exp}, {@code jti}). Tokens are cached per audience and reissued shortly before they expire.
 */
public class LocalServiceTokenProvider implements ServiceTokenProvider {

  private static final Duration RENEWAL_MARGIN = Duration.ofSeconds(30);

  private final String issuer;
  private final byte[] secret;
  private final Duration timeToLive;
  private final Clock clock;
  private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

  public LocalServiceTokenProvider(String issuer, byte[] secret, Duration timeToLive, Clock clock) {
    this.issuer = issuer;
    this.secret = secret.clone();
    this.timeToLive = timeToLive;
    this.clock = clock;
  }

  @Override
  public String tokenFor(String audience) {
    Instant now = clock.instant();
    CachedToken cached = cache.get(audience);
    if (cached != null && cached.expiresAt.minus(RENEWAL_MARGIN).isAfter(now)) {
      return cached.value;
    }
    CachedToken fresh = sign(audience, now);
    cache.put(audience, fresh);
    return fresh.value;
  }

  private CachedToken sign(String audience, Instant now) {
    Instant expiresAt = now.plus(timeToLive);
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .jwtID(UUID.randomUUID().toString())
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    try {
      jwt.sign(new MACSigner(secret));
    } catch (JOSEException exception) {
      throw new IllegalStateException("Cannot sign service token", exception);
    }
    return new CachedToken(jwt.serialize(), expiresAt);
  }

  private record CachedToken(String value, Instant expiresAt) {}
}
