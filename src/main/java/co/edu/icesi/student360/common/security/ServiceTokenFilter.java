package co.edu.icesi.student360.common.security;

import co.edu.icesi.student360.common.logging.Correlation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests on protected paths that do not carry a valid service token. This is what makes
 * the identity headers trustworthy: a client cannot reach a domain service by forging {@code
 * X-User-Id}, because it would also have to forge the token only the gateway and sibling services
 * can mint. Public paths (JWKS, health) are simply not listed as protected.
 */
public class ServiceTokenFilter extends OncePerRequestFilter {

  public static final String CALLER_ATTRIBUTE = ServiceTokenFilter.class.getName() + ".caller";
  private static final Logger log = LoggerFactory.getLogger(ServiceTokenFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final ServiceTokenValidator validator;
  private final List<String> protectedPaths;
  private final ObjectMapper objectMapper;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public ServiceTokenFilter(
      ServiceTokenValidator validator, List<String> protectedPaths, ObjectMapper objectMapper) {
    this.validator = validator;
    this.protectedPaths = List.copyOf(protectedPaths);
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return protectedPaths.stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      reject(response, "Missing service token");
      return;
    }
    try {
      ServiceIdentity caller = validator.validate(header.substring(BEARER_PREFIX.length()));
      request.setAttribute(CALLER_ATTRIBUTE, caller);
    } catch (InvalidServiceTokenException exception) {
      log.warn("Rejected service token: {}", exception.getMessage());
      reject(response, exception.getMessage());
      return;
    }
    chain.doFilter(request, response);
  }

  private void reject(HttpServletResponse response, String detail) throws IOException {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    problem.setTitle("Authentication failed");
    Correlation.currentRequestId().ifPresent(id -> problem.setProperty("requestId", id));
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
