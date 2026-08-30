package co.edu.icesi.student360.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Honours an incoming {@code X-Request-Id} or generates one, exposes it in the MDC for every log
 * line of the request and echoes it on the response so a caller can quote it. Runs first: nothing
 * that happens during a request may be logged without it.
 */
public class CorrelationFilter extends OncePerRequestFilter {

  /** Only ids we would have generated ourselves are honoured; anything else is replaced. */
  private static final Pattern ACCEPTED_ID = Pattern.compile("^[A-Za-z0-9._-]{8,128}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String requestId = resolveRequestId(request.getHeader(Correlation.REQUEST_ID_HEADER));
    MDC.put(MdcKeys.REQUEST_ID, requestId);
    response.setHeader(Correlation.REQUEST_ID_HEADER, requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MdcKeys.REQUEST_ID);
    }
  }

  static String resolveRequestId(String incoming) {
    if (incoming != null && ACCEPTED_ID.matcher(incoming).matches()) {
      return incoming;
    }
    return UUID.randomUUID().toString();
  }
}
