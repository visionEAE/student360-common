# student360-common

Shared foundations for every Student 360° service. Everything that becomes a managed cloud service
in stage 2 is a **port** here with a **local adapter**; services depend on the port only.

| Concern | What you get | Port → stage 1 adapter → stage 2 adapter |
|---|---|---|
| Operational log | `logback-spring.xml`: one JSON object per line on stdout with `timestamp`, `level`, `service`, `traceId`, `spanId`, `requestId`, `userId`, `message` | stdout → Cloud Logging (no code change) |
| Correlation | `CorrelationFilter` honours or generates `X-Request-Id`, puts it in the MDC, echoes it on the response; `Correlation.currentRequestId()` | — |
| Tracing | Micrometer Tracing defaults (W3C `traceparent`, 100% sampling, no exporter) | exporter: none → Cloud Trace |
| Identity | `IdentityHeaderFilter` → `IdentityContext.current()` / `require()` from `X-User-Id`, `X-User-Roles`, `X-External-Reference` | — |
| Service-to-service auth | `ServiceTokenProvider` / `ServiceTokenValidator`; `ServiceTokenFilter` rejects protected paths without a valid token (401) | `Local*` HS256 shared secret → Google ID token |
| Audit trail | `@Audited` + `AuditAspect` → `AuditTrail` → `AuditWriter` (ALLOWED and DENIED, with `authorization_basis`) | `JdbcAuditWriter` into `audit.audit_record` → + Cloud Storage export |
| Domain events | `EventPublisher.publish(DomainEvent)` | `OutboxEventPublisher` (caller's transaction) → Pub/Sub relay |
| Errors | `GlobalExceptionHandler` → RFC 7807 `ProblemDetail` carrying `requestId`; `NotFoundException`, `AccessDeniedForSubjectException`, `AuthenticationFailedException`, `RateLimitExceededException` | — |

## Using it from a service

```xml
<dependency>
  <groupId>co.edu.icesi.student360</groupId>
  <artifactId>student360-common</artifactId>
  <version>${student360-common.version}</version>
</dependency>
```

```yaml
student360:
  service-name: core-service            # also the audience of inbound service tokens
  security:
    service-token:
      secret: ${SERVICE_TOKEN_SECRET}   # ≥ 32 bytes, shared by the services in stage 1
      protected-paths: ["/api/**"]      # everything else (JWKS, actuator) is public
  outbox:
    table: outbox_event                 # created by the service's own Flyway migration
```

Auto-configuration does the rest: filters (correlation → service token → identity), audit writer
and aspect (when a `DataSource` exists), outbox publisher, exception handler, tracing defaults.

```java
@Audited(action = "READ_FINANCIAL_STATUS", subjectType = "STUDENT")
@Transactional(readOnly = true)
public FinancialStatus findFinancialStatus(String studentId) {
  studentAccessPolicy.check(studentId);            // throws AccessDeniedForSubjectException → DENIED row + 403
  AuthorizationBasisHolder.grant(SELF);            // recorded in the ALLOWED row
  ...
}
```

## Resolving the artifact

* **Local development:** `make build-common` in `student360-infra` (runs `mvn install` here).
* **CI / cloud builds:** published to GitHub Packages on every `v*` tag by `.github/workflows/publish.yml`.

## Build

```
mvn verify     # google-java-format check, checkstyle, unit tests, Testcontainers integration test (needs Docker)
```

The integration test (`FoundationsIntegrationTest`) is phase gate 0.B: a throwaway service using
the library emits JSON logs carrying the request id, refuses calls without a service token, and
leaves audit and outbox rows in a real PostgreSQL — including the DENIED row of a rolled-back
request.
