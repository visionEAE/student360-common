package co.edu.icesi.student360.common.config;

import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import co.edu.icesi.student360.common.security.ServiceTokenValidator;
import co.edu.icesi.student360.common.security.google.GoogleServiceTokenProvider;
import co.edu.icesi.student360.common.security.google.GoogleServiceTokenValidator;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Stage 2 service-to-service authentication: Google-signed ID tokens on Cloud Run, activated by
 * {@code student360.security.service-token.mode=google}. Which half a service gets follows from its
 * configuration, not from code: a caller (the gateway, support, network) declares an {@code
 * audience-map} and receives a provider; a callee (core, lms, support, network) declares {@code
 * expected-audience} — its own URL — and receives a validator. Ordered before the local
 * auto-configuration so its {@code @ConditionalOnMissingBean} defaults yield; the local beans are
 * additionally mode-gated, so the two adapter pairs can never coexist.
 */
@AutoConfiguration(before = ServiceTokenAutoConfiguration.class)
@EnableConfigurationProperties(Student360Properties.class)
@ConditionalOnProperty(name = "student360.security.service-token.mode", havingValue = "google")
public class GoogleServiceTokenAutoConfiguration {

  static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

  @Bean
  @ConditionalOnMissingBean
  public Clock student360Clock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnMissingBean
  @Conditional(OnAudienceMapCondition.class)
  public ServiceTokenProvider googleServiceTokenProvider(Student360Properties properties)
      throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
    if (!(credentials instanceof IdTokenProvider identity)) {
      throw new IllegalStateException(
          "Application Default Credentials cannot mint ID tokens — on Cloud Run this is the"
              + " runtime service account; locally use a service-account impersonation ADC");
    }
    return new GoogleServiceTokenProvider(
        identity, properties.security().serviceToken().audienceMap());
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty("student360.security.service-token.expected-audience")
  public ServiceTokenValidator googleServiceTokenValidator(
      Student360Properties properties, Clock clock) throws IOException {
    JWKSource<SecurityContext> keys =
        JWKSourceBuilder.create(new URL(GOOGLE_JWKS_URL)).cache(true).retrying(true).build();
    return new GoogleServiceTokenValidator(
        keys,
        properties.security().serviceToken().expectedAudience(),
        properties.security().serviceToken().allowedCallers(),
        clock);
  }

  /**
   * A caller is whoever declared at least one audience-map entry. A plain
   * {@code @ConditionalOnProperty} cannot ask "is this map non-empty", and gating on one well-known
   * key would break the first caller that does not happen to call that service.
   */
  static class OnAudienceMapCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !Binder.get(context.getEnvironment())
          .bind(
              "student360.security.service-token.audience-map",
              Bindable.mapOf(String.class, String.class))
          .orElse(Map.of())
          .isEmpty();
    }
  }
}
