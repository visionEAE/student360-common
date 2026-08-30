package co.edu.icesi.student360.common.security.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.IdTokenProvider;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoogleServiceTokenProviderTest {

  /** A stand-in for the metadata server: records the audiences it was asked for. */
  static final class FakeIdentity implements IdTokenProvider {
    private final List<String> requestedAudiences = new ArrayList<>();

    @Override
    public IdToken idTokenWithAudience(String audience, List<Option> options) {
      requestedAudiences.add(audience);
      try {
        RSAKey key = new RSAKeyGenerator(2048).keyID("k").generate();
        SignedJWT jwt =
            new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k").build(),
                new JWTClaimsSet.Builder()
                    .audience(audience)
                    .issuer("https://accounts.google.com")
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .build());
        jwt.sign(new RSASSASigner(key));
        return IdToken.create(jwt.serialize());
      } catch (Exception exception) {
        throw new IllegalStateException(exception);
      }
    }
  }

  @Test
  void shouldMintATokenWhoseAudienceIsTheMappedUrlNotTheLogicalName() {
    FakeIdentity identity = new FakeIdentity();
    GoogleServiceTokenProvider provider =
        new GoogleServiceTokenProvider(
            identity, Map.of("core-service", "https://s360-core-x.us-central1.run.app"));

    String token = provider.tokenFor("core-service");

    assertThat(token).isNotBlank();
    assertThat(identity.requestedAudiences)
        .containsExactly("https://s360-core-x.us-central1.run.app");
  }

  @Test
  void shouldFailFastWhenTheLogicalNameIsNotMapped() {
    GoogleServiceTokenProvider provider =
        new GoogleServiceTokenProvider(new FakeIdentity(), Map.of());
    assertThatThrownBy(() -> provider.tokenFor("core-service"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("audience-map");
  }
}
