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
 * Stage 1 service-to-service authentication. A service that defines its own {@link
 * ServiceTokenProvider} or {@link ServiceTokenValidator} bean (the stage 2 Google adapters) wins
 * over these defaults without touching this class.
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
