package co.edu.icesi.student360.common.security.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.icesi.student360.common.security.InvalidServiceTokenException;
import co.edu.icesi.student360.common.security.ServiceIdentity;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LocalServiceTokenTest {

  private static final byte[] SECRET =
      "0123456789abcdef0123456789abcdef-dev-only".getBytes(StandardCharsets.UTF_8);
  private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

  private final LocalServiceTokenProvider gateway =
      new LocalServiceTokenProvider("gateway", SECRET, Duration.ofMinutes(5), fixed(NOW));

  @Test
  void shouldAcceptTokenMintedForThisService() {
    String token = gateway.tokenFor("core-service");
    LocalServiceTokenValidator core =
        new LocalServiceTokenValidator("core-service", SECRET, fixed(NOW));

    ServiceIdentity identity = core.validate(token);

    assertThat(identity).isEqualTo(new ServiceIdentity("gateway", "core-service"));
  }

  @Test
  void shouldRejectTokenMintedForAnotherService() {
    String token = gateway.tokenFor("core-service");
    LocalServiceTokenValidator lms =
        new LocalServiceTokenValidator("lms-service", SECRET, fixed(NOW));

    assertThatThrownBy(() -> lms.validate(token))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("not intended");
  }

  @Test
  void shouldRejectExpiredToken() {
    String token = gateway.tokenFor("core-service");
    LocalServiceTokenValidator later =
        new LocalServiceTokenValidator(
            "core-service", SECRET, fixed(NOW.plus(Duration.ofMinutes(6))));

    assertThatThrownBy(() -> later.validate(token))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("Expired");
  }

  @Test
  void shouldRejectTokenSignedWithAnotherSecret() {
    byte[] otherSecret = "ffffffffffffffffffffffffffffffff-other".getBytes(StandardCharsets.UTF_8);
    String token =
        new LocalServiceTokenProvider("intruder", otherSecret, Duration.ofMinutes(5), fixed(NOW))
            .tokenFor("core-service");
    LocalServiceTokenValidator core =
        new LocalServiceTokenValidator("core-service", SECRET, fixed(NOW));

    assertThatThrownBy(() -> core.validate(token))
        .isInstanceOf(InvalidServiceTokenException.class)
        .hasMessageContaining("signature");
  }

  @Test
  void shouldRejectGarbage() {
    LocalServiceTokenValidator core =
        new LocalServiceTokenValidator("core-service", SECRET, fixed(NOW));

    assertThatThrownBy(() -> core.validate("not.a.jwt"))
        .isInstanceOf(InvalidServiceTokenException.class);
  }

  @Test
  void shouldReuseCachedTokenUntilItNearsExpiry() {
    String first = gateway.tokenFor("core-service");
    String second = gateway.tokenFor("core-service");

    assertThat(second).isEqualTo(first);
  }

  private static Clock fixed(Instant instant) {
    return Clock.fixed(instant, ZoneOffset.UTC);
  }
}
