package co.edu.icesi.student360.common.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
      /**
       * Which adapter pair backs the ports: {@code local} (stage 1, shared HS256 secret) or {@code
       * google} (stage 2, Google-signed ID tokens on Cloud Run). Defaults to {@code local} so a
       * developer machine needs no extra configuration; production sets {@code google} explicitly.
       */
      @DefaultValue("local") String mode,
      /** Shared HS256 secret for stage 1. Absent means the service neither mints nor checks. */
      String secret,
      @DefaultValue("PT5M") Duration timeToLive,
      /** Ant patterns that require a valid inbound service token. */
      @DefaultValue({"/api/**"}) List<String> protectedPaths,
      /**
       * google mode, callers only: logical service name → the callee's own URL, which is the
       * audience a Google ID token must carry. Fed from the same {@code *_SERVICE_URL} variables
       * the HTTP clients already use, so the two can never disagree.
       */
      @DefaultValue Map<String, String> audienceMap,
      /** google mode, callees only: this service's own URL — the audience it accepts. */
      String expectedAudience,
      /**
       * google mode, callees only: service-account emails allowed to call. Empty means any
       * Google-signed token with the right audience is accepted (Cloud Run IAM already restricts
       * who can obtain one for a private service).
       */
      @DefaultValue List<String> allowedCallers) {}

  public record Outbox(@DefaultValue("outbox_event") String table) {}
}
