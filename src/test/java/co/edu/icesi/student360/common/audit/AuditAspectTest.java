package co.edu.icesi.student360.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.icesi.student360.common.api.exception.AccessDeniedForSubjectException;
import co.edu.icesi.student360.common.identity.Identity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/** Pure AspectJ proxy test: no Spring context, no database. */
class AuditAspectTest {

  private final List<AuditRecord> written = new ArrayList<>();
  private final AuditTrail trail =
      new AuditTrail(written::add, "test-service", Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
  private final SampleService service = proxied(new SampleService());

  @Test
  void shouldRecordAllowedOutcomeWithTheBasisDeclaredByThePolicy() {
    String result = service.readFinancialStatus("S-1001");

    assertThat(result).isEqualTo("status of S-1001");
    assertThat(written).hasSize(1);
    AuditRecord record = written.get(0);
    assertThat(record.action()).isEqualTo("READ_FINANCIAL_STATUS");
    assertThat(record.subjectType()).isEqualTo("STUDENT");
    assertThat(record.subjectId()).isEqualTo("S-1001");
    assertThat(record.outcome()).isEqualTo(Outcome.ALLOWED);
    assertThat(record.authorizationBasis()).isEqualTo(AuthorizationBasis.SELF);
    assertThat(record.serviceName()).isEqualTo("test-service");
    assertThat(record.requestId()).isEqualTo(AuditTrail.NO_REQUEST);
  }

  @Test
  void shouldRecordDeniedOutcomeAndRethrow() {
    assertThatThrownBy(() -> service.readFinancialStatus("S-9999"))
        .isInstanceOf(AccessDeniedForSubjectException.class);

    assertThat(written).hasSize(1);
    AuditRecord record = written.get(0);
    assertThat(record.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(record.authorizationBasis()).isEqualTo(AuthorizationBasis.NONE);
    assertThat(record.subjectId()).isEqualTo("S-9999");
    assertThat(record.details()).containsKey("reason");
  }

  @Test
  void shouldRecordWithExplicitActorWhenNoIdentityIsBound() {
    Identity actor = new Identity(UUID.randomUUID(), Set.of("STUDENT"), "S-1001");

    trail.recordAs(
        actor,
        RecordType.SECURITY,
        "LOGIN_SUCCEEDED",
        "SESSION",
        "s-1",
        null,
        Outcome.ALLOWED,
        Map.of());

    assertThat(written.get(0).actorId()).isEqualTo(actor.userId());
    assertThat(written.get(0).actorRoles()).containsExactly("STUDENT");
  }

  @Test
  void shouldResolveSubjectIdByParameterName() {
    service.assignAdvisor("A-2001", "S-1001");

    assertThat(written.get(0).subjectId()).isEqualTo("S-1001");
    assertThat(written.get(0).recordType()).isEqualTo(RecordType.STATE_CHANGE);
  }

  private SampleService proxied(SampleService target) {
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.addAspect(new AuditAspect(trail));
    return factory.getProxy();
  }

  static class SampleService {

    @Audited(action = "READ_FINANCIAL_STATUS", subjectType = "STUDENT")
    public String readFinancialStatus(String studentId) {
      if (!"S-1001".equals(studentId)) {
        throw new AccessDeniedForSubjectException("STUDENT", studentId);
      }
      AuthorizationBasisHolder.grant(AuthorizationBasis.SELF);
      return "status of " + studentId;
    }

    @Audited(
        action = "ASSIGN_ADVISOR",
        subjectType = "STUDENT",
        recordType = RecordType.STATE_CHANGE,
        subjectIdParameter = "studentId")
    public void assignAdvisor(String advisorId, String studentId) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.ADMIN_ROLE);
    }
  }
}
