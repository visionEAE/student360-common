package co.edu.icesi.student360.common.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.icesi.student360.common.api.exception.AccessDeniedForSubjectException;
import co.edu.icesi.student360.common.api.exception.AuthenticationFailedException;
import co.edu.icesi.student360.common.audit.AuthorizationBasis;
import co.edu.icesi.student360.common.audit.AuthorizationBasisHolder;
import co.edu.icesi.student360.common.identity.Identity;
import co.edu.icesi.student360.common.identity.IdentityContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StudentRecordAccessPolicyTest {

  private final StudentRecordAccessPolicy policy = new StudentRecordAccessPolicy();

  @AfterEach
  void clear() {
    IdentityContext.clear();
    AuthorizationBasisHolder.clear();
  }

  @Test
  void shouldGrantSelfWhenStudentReadsOwnRecord() {
    IdentityContext.set(new Identity(UUID.randomUUID(), Set.of("STUDENT"), "S-1001"));

    policy.assertCanRead("S-1001");

    assertThat(AuthorizationBasisHolder.current()).contains(AuthorizationBasis.SELF);
  }

  @Test
  void shouldDenyStudentReadingAnotherStudent() {
    IdentityContext.set(new Identity(UUID.randomUUID(), Set.of("STUDENT"), "S-1001"));

    assertThatThrownBy(() -> policy.assertCanRead("S-1003"))
        .isInstanceOf(AccessDeniedForSubjectException.class)
        .hasMessageContaining("S-1003");
    assertThat(AuthorizationBasisHolder.current()).isEmpty();
  }

  @Test
  void shouldGrantStaffRoleToAdvisorsAndAdminRoleToAdmins() {
    IdentityContext.set(new Identity(UUID.randomUUID(), Set.of("ADVISOR"), "A-2001"));
    policy.assertCanRead("S-1003");
    assertThat(AuthorizationBasisHolder.current()).contains(AuthorizationBasis.STAFF_ROLE);

    IdentityContext.set(new Identity(UUID.randomUUID(), Set.of("ADMIN"), null));
    policy.assertCanRead("S-1003");
    assertThat(AuthorizationBasisHolder.current()).contains(AuthorizationBasis.ADMIN_ROLE);
  }

  @Test
  void shouldRequireAnIdentity() {
    assertThatThrownBy(() -> policy.assertCanRead("S-1001"))
        .isInstanceOf(AuthenticationFailedException.class);
  }
}
