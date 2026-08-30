package co.edu.icesi.student360.common.identity;

import co.edu.icesi.student360.common.logging.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Turns the identity headers injected by the gateway into an {@link Identity} for the duration of
 * the request. The headers are trusted only because the service token filter, which runs before
 * this one on protected paths, has already proven the caller is the gateway or a sibling service.
 */
public class IdentityHeaderFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Optional<Identity> identity = parse(request);
    identity.ifPresent(
        value -> {
          IdentityContext.set(value);
          MDC.put(MdcKeys.USER_ID, value.userId().toString());
        });
    try {
      chain.doFilter(request, response);
    } finally {
      IdentityContext.clear();
      MDC.remove(MdcKeys.USER_ID);
    }
  }

  static Optional<Identity> parse(HttpServletRequest request) {
    String userId = request.getHeader(IdentityHeaders.USER_ID);
    if (userId == null || userId.isBlank()) {
      return Optional.empty();
    }
    try {
      Set<String> roles =
          Optional.ofNullable(request.getHeader(IdentityHeaders.USER_ROLES))
              .map(header -> Arrays.stream(header.split(",")))
              .map(
                  stream ->
                      stream
                          .map(String::trim)
                          .filter(role -> !role.isEmpty())
                          .collect(Collectors.toUnmodifiableSet()))
              .orElse(Set.of());
      String reference = request.getHeader(IdentityHeaders.EXTERNAL_REFERENCE);
      return Optional.of(new Identity(UUID.fromString(userId), roles, reference));
    } catch (IllegalArgumentException malformedUserId) {
      return Optional.empty();
    }
  }
}
