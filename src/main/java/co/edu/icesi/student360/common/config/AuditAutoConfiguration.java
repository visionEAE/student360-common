package co.edu.icesi.student360.common.config;

import co.edu.icesi.student360.common.audit.AuditAspect;
import co.edu.icesi.student360.common.audit.AuditTrail;
import co.edu.icesi.student360.common.audit.AuditWriter;
import co.edu.icesi.student360.common.audit.jdbc.JdbcAuditWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Audit trail wiring, active in any service with a JDBC data source. */
@AutoConfiguration(
    after = {
      JdbcTemplateAutoConfiguration.class,
      DataSourceTransactionManagerAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      ServiceTokenAutoConfiguration.class
    })
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean({JdbcTemplate.class, PlatformTransactionManager.class})
public class AuditAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public AuditWriter auditWriter(
      JdbcTemplate jdbcTemplate,
      PlatformTransactionManager transactionManager,
      ObjectMapper objectMapper) {
    return new JdbcAuditWriter(jdbcTemplate, transactionManager, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditTrail auditTrail(AuditWriter writer, Student360Properties properties, Clock clock) {
    return new AuditTrail(
        writer, ServiceTokenAutoConfiguration.requireServiceName(properties), clock);
  }

  @Bean
  @ConditionalOnClass(Aspect.class)
  @ConditionalOnMissingBean
  public AuditAspect auditAspect(AuditTrail trail) {
    return new AuditAspect(trail);
  }
}
