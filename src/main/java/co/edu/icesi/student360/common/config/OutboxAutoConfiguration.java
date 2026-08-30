package co.edu.icesi.student360.common.config;

import co.edu.icesi.student360.common.outbox.EventPublisher;
import co.edu.icesi.student360.common.outbox.OutboxEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Outbox wiring. A service that needs the stage 2 relay defines its own {@link EventPublisher}; the
 * domain code that calls {@code publish} stays untouched.
 */
@AutoConfiguration(
    after = {JdbcTemplateAutoConfiguration.class, ServiceTokenAutoConfiguration.class})
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean(JdbcTemplate.class)
public class OutboxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public EventPublisher eventPublisher(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      Clock clock,
      Student360Properties properties) {
    return new OutboxEventPublisher(jdbcTemplate, objectMapper, clock, properties.outbox().table());
  }
}
