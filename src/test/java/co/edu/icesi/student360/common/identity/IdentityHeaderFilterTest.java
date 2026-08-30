package co.edu.icesi.student360.common.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IdentityHeaderFilterTest {

  private final IdentityHeaderFilter filter = new IdentityHeaderFilter();

  @Test
  void shouldExposeIdentityDuringTheRequestAndClearItAfterwards() throws Exception {
    UUID userId = UUID.randomUUID();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(IdentityHeaders.USER_ID, userId.toString());
    request.addHeader(IdentityHeaders.USER_ROLES, "STUDENT, ADVISOR");
    request.addHeader(IdentityHeaders.EXTERNAL_REFERENCE, "S-1001");
    AtomicReference<Optional<Identity>> seen = new AtomicReference<>();

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            seen.set(IdentityContext.current());
          }
        });

    assertThat(seen.get()).isPresent();
    Identity identity = seen.get().orElseThrow();
    assertThat(identity.userId()).isEqualTo(userId);
    assertThat(identity.roles()).containsExactlyInAnyOrder("STUDENT", "ADVISOR");
    assertThat(identity.externalReference()).isEqualTo("S-1001");
    assertThat(IdentityContext.current()).isEmpty();
  }

  @Test
  void shouldIgnoreMalformedUserIdInsteadOfFailingTheRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(IdentityHeaders.USER_ID, "not-a-uuid");

    assertThat(IdentityHeaderFilter.parse(request)).isEmpty();
  }
}
