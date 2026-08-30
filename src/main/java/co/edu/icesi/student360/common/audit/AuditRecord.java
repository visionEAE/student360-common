package co.edu.icesi.student360.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** One row of {@code audit.audit_record}. Immutable; built through {@link #builder()}. */
public record AuditRecord(
    Instant occurredAt,
    String requestId,
    String traceId,
    String serviceName,
    RecordType recordType,
    String action,
    UUID actorId,
    Set<String> actorRoles,
    String subjectType,
    String subjectId,
    AuthorizationBasis authorizationBasis,
    Outcome outcome,
    String sourceIp,
    Map<String, Object> details) {

  public AuditRecord {
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(serviceName, "serviceName");
    Objects.requireNonNull(recordType, "recordType");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(outcome, "outcome");
    actorRoles = actorRoles == null ? Set.of() : Set.copyOf(actorRoles);
    details = details == null ? Map.of() : Map.copyOf(details);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Fluent construction; the record itself stays a plain value. */
  public static final class Builder {
    private Instant occurredAt;
    private String requestId;
    private String traceId;
    private String serviceName;
    private RecordType recordType;
    private String action;
    private UUID actorId;
    private Set<String> actorRoles;
    private String subjectType;
    private String subjectId;
    private AuthorizationBasis authorizationBasis;
    private Outcome outcome;
    private String sourceIp;
    private Map<String, Object> details;

    private Builder() {}

    public Builder occurredAt(Instant value) {
      this.occurredAt = value;
      return this;
    }

    public Builder requestId(String value) {
      this.requestId = value;
      return this;
    }

    public Builder traceId(String value) {
      this.traceId = value;
      return this;
    }

    public Builder serviceName(String value) {
      this.serviceName = value;
      return this;
    }

    public Builder recordType(RecordType value) {
      this.recordType = value;
      return this;
    }

    public Builder action(String value) {
      this.action = value;
      return this;
    }

    public Builder actorId(UUID value) {
      this.actorId = value;
      return this;
    }

    public Builder actorRoles(Set<String> value) {
      this.actorRoles = value;
      return this;
    }

    public Builder subjectType(String value) {
      this.subjectType = value;
      return this;
    }

    public Builder subjectId(String value) {
      this.subjectId = value;
      return this;
    }

    public Builder authorizationBasis(AuthorizationBasis value) {
      this.authorizationBasis = value;
      return this;
    }

    public Builder outcome(Outcome value) {
      this.outcome = value;
      return this;
    }

    public Builder sourceIp(String value) {
      this.sourceIp = value;
      return this;
    }

    public Builder details(Map<String, Object> value) {
      this.details = value;
      return this;
    }

    public AuditRecord build() {
      return new AuditRecord(
          occurredAt,
          requestId,
          traceId,
          serviceName,
          recordType,
          action,
          actorId,
          actorRoles,
          subjectType,
          subjectId,
          authorizationBasis,
          outcome,
          sourceIp,
          details);
    }
  }
}
