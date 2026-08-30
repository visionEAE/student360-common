package co.edu.icesi.student360.common.config;

import co.edu.icesi.student360.common.api.GlobalExceptionHandler;
import co.edu.icesi.student360.common.identity.IdentityHeaderFilter;
import co.edu.icesi.student360.common.logging.CorrelationFilter;
import co.edu.icesi.student360.common.security.ServiceTokenFilter;
import co.edu.icesi.student360.common.security.ServiceTokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Servlet-side wiring. Filter order is the contract: correlation first (every log line needs the
 * request id), then the service token gate, then identity headers (trusted only past the gate).
 */
@AutoConfiguration(after = ServiceTokenAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

  static final int CORRELATION_ORDER = Ordered.HIGHEST_PRECEDENCE;
  static final int SERVICE_TOKEN_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
  static final int IDENTITY_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

  @Bean
  public FilterRegistrationBean<CorrelationFilter> correlationFilter() {
    FilterRegistrationBean<CorrelationFilter> registration =
        new FilterRegistrationBean<>(new CorrelationFilter());
    registration.setOrder(CORRELATION_ORDER);
    return registration;
  }

  @Bean
  @ConditionalOnBean(ServiceTokenValidator.class)
  public FilterRegistrationBean<ServiceTokenFilter> serviceTokenFilter(
      ServiceTokenValidator validator, Student360Properties properties, ObjectMapper objectMapper) {
    FilterRegistrationBean<ServiceTokenFilter> registration =
        new FilterRegistrationBean<>(
            new ServiceTokenFilter(
                validator, properties.security().serviceToken().protectedPaths(), objectMapper));
    registration.setOrder(SERVICE_TOKEN_ORDER);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<IdentityHeaderFilter> identityHeaderFilter() {
    FilterRegistrationBean<IdentityHeaderFilter> registration =
        new FilterRegistrationBean<>(new IdentityHeaderFilter());
    registration.setOrder(IDENTITY_ORDER);
    return registration;
  }

  @Bean
  @ConditionalOnMissingBean
  public GlobalExceptionHandler globalExceptionHandler() {
    return new GlobalExceptionHandler();
  }
}
