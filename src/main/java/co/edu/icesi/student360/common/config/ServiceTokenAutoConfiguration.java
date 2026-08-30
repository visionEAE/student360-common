package co.edu.icesi.student360.common.config;

import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import co.edu.icesi.student360.common.security.ServiceTokenValidator;
import co.edu.icesi.student360.common.security.local.LocalServiceTokenProvider;
import co.edu.icesi.student360.common.security.local.LocalServiceTokenValidator;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Stage 1 service-to-service authentication. The local HS256 adapters exist only while {@code
 * student360.security.service-token.mode} is {@code local} (the default) — setting {@code google}
 * hands both ports to {@link GoogleServiceTokenAutoConfiguration} without touching this class. The
 * mode gate matters because {@code ServiceTokenFilter} only protects {@code /api/**} when a
 * validator bean exists: dropping the secret alone must never silently unprotect a service, it must
 * be an explicit mode switch to an adapter that still validates.
 */
@AutoConfiguration
@EnableConfigurationProperties(Student360Properties.class)
public class ServiceTokenAutoConfiguration {

  private static final int MINIMUM_SECRET_BYTES = 32;

  @Bean
  @ConditionalOnMissingBean
  public Clock student360Clock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty("student360.security.service-token.secret")
  @ConditionalOnProperty(
      name = "student360.security.service-token.mode",
      havingValue = "local",
      matchIfMissing = true)
  public ServiceTokenProvider serviceTokenProvider(Student360Properties properties, Clock clock) {
    return new LocalServiceTokenProvider(
        requireServiceName(properties),
        secret(properties),
        properties.security().serviceToken().timeToLive(),
        clock);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty("student360.security.service-token.secret")
  @ConditionalOnProperty(
      name = "student360.security.service-token.mode",
      havingValue = "local",
      matchIfMissing = true)
  public ServiceTokenValidator serviceTokenValidator(Student360Properties properties, Clock clock) {
    return new LocalServiceTokenValidator(
        requireServiceName(properties), secret(properties), clock);
  }

  static String requireServiceName(Student360Properties properties) {
    if (properties.serviceName() == null || properties.serviceName().isBlank()) {
      throw new IllegalStateException("student360.service-name must be set");
    }
    return properties.serviceName();
  }

  private static byte[] secret(Student360Properties properties) {
    byte[] bytes = properties.security().serviceToken().secret().getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MINIMUM_SECRET_BYTES) {
      throw new IllegalStateException(
          "student360.security.service-token.secret must be at least 32 bytes");
    }
    return bytes;
  }
}
