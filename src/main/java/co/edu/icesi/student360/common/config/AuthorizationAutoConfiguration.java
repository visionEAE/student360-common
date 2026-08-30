package co.edu.icesi.student360.common.config;

import co.edu.icesi.student360.common.authorization.StudentRecordAccessPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** Shared access policies; a service may override any of them with its own bean. */
@AutoConfiguration
public class AuthorizationAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public StudentRecordAccessPolicy studentRecordAccessPolicy() {
    return new StudentRecordAccessPolicy();
  }
}
