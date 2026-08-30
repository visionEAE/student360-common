package co.edu.icesi.student360.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationFilterTest {

  private final CorrelationFilter filter = new CorrelationFilter();

  @Test
  void shouldHonourIncomingRequestIdAndEchoItOnTheResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(Correlation.REQUEST_ID_HEADER, "demo-request-0001");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInsideChain = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            seenInsideChain.set(MDC.get(MdcKeys.REQUEST_ID));
          }
        });

    assertThat(seenInsideChain.get()).isEqualTo("demo-request-0001");
    assertThat(response.getHeader(Correlation.REQUEST_ID_HEADER)).isEqualTo("demo-request-0001");
    assertThat(MDC.get(MdcKeys.REQUEST_ID)).as("MDC is cleared after the request").isNull();
  }

  @Test
  void shouldGenerateRequestIdWhenIncomingOneIsMissingOrUnsafe() throws Exception {
    assertThat(CorrelationFilter.resolveRequestId(null)).hasSize(36);
    assertThat(CorrelationFilter.resolveRequestId("bad id with spaces")).hasSize(36);
    assertThat(CorrelationFilter.resolveRequestId("<script>")).hasSize(36);
    assertThat(CorrelationFilter.resolveRequestId("abc")).as("too short").hasSize(36);
  }
}
