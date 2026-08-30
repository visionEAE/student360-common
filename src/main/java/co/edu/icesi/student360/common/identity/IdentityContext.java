package co.edu.icesi.student360.common.identity;

import co.edu.icesi.student360.common.api.exception.AuthenticationFailedException;
import java.util.Optional;

/**
 * Thread-bound holder of the current {@link Identity}. Populated by {@link IdentityHeaderFilter}
 * and cleared when the request ends; domain services read it through {@link #current()} or {@link
 * #require()} without knowing where the identity came from.
 */
public final class IdentityContext {

  private static final ThreadLocal<Identity> CURRENT = new ThreadLocal<>();

  private IdentityContext() {}

  public static Optional<Identity> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static Identity require() {
    return current().orElseThrow(() -> new AuthenticationFailedException("No user identity"));
  }

  public static void set(Identity identity) {
    CURRENT.set(identity);
  }

  public static void clear() {
    CURRENT.remove();
  }
}
