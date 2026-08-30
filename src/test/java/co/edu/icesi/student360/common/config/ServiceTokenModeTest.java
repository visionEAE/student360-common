package co.edu.icesi.student360.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import co.edu.icesi.student360.common.security.ServiceTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The mode gate: local HS256 beans exist only while {@code mode} is {@code local} (or absent). The
 * dangerous path this guards is dropping the secret in production without a replacement — the
 * filter only protects {@code /api/**} when a validator bean exists, so the switch away from the
 * local adapters must be an explicit {@code mode}, never a silently missing property.
 */
class ServiceTokenModeTest {

  private static final String SECRET =
      "student360.security.service-token.secret=0123456789abcdef0123456789abcdef";

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ServiceTokenAutoConfiguration.class))
          .withPropertyValues("student360.service-name=probe-service");

  @Test
  void shouldCreateLocalBeansWhenModeIsAbsentAndASecretIsSet() {
    runner
        .withPropertyValues(SECRET)
        .run(
            context -> {
              assertThat(context).hasSingleBean(ServiceTokenProvider.class);
              assertThat(context).hasSingleBean(ServiceTokenValidator.class);
            });
  }

  @Test
  void shouldCreateLocalBeansWhenModeIsExplicitlyLocal() {
    runner
        .withPropertyValues(SECRET, "student360.security.service-token.mode=local")
        .run(context -> assertThat(context).hasSingleBean(ServiceTokenValidator.class));
  }

  @Test
  void shouldNotCreateLocalBeansInGoogleModeEvenIfASecretIsStillSet() {
    // A leftover SERVICE_TOKEN_SECRET in the environment must not resurrect the HS256 pair.
    runner
        .withPropertyValues(SECRET, "student360.security.service-token.mode=google")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(ServiceTokenProvider.class);
              assertThat(context).doesNotHaveBean(ServiceTokenValidator.class);
            });
  }

  @Test
  void shouldNotCreateLocalBeansWithoutASecret() {
    runner.run(context -> assertThat(context).doesNotHaveBean(ServiceTokenProvider.class));
  }
}
