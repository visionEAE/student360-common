package co.edu.icesi.student360.common.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Everything the shared foundations need from a service, under {@code student360.*}. Values come
 * from {@code application.yml} and environment variables; none is hardcoded in code.
 */
@ConfigurationProperties(prefix = "student360")
public record Student360Properties(
    /** Logical name of this service ({@code core-service}); also the service token audience. */
    String serviceName, @DefaultValue Security security, @DefaultValue Outbox outbox) {

  public record Security(@DefaultValue ServiceToken serviceToken) {}

  public record ServiceToken(
      /** Shared HS256 secret for stage 1. Absent means the service neither mints nor checks. */
      String secret,
      @DefaultValue("PT5M") Duration timeToLive,
      /** Ant patterns that require a valid inbound service token. */
      @DefaultValue({"/api/**"}) List<String> protectedPaths) {}

  public record Outbox(@DefaultValue("outbox_event") String table) {}
}
