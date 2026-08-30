package co.edu.icesi.student360.common.audit;

import java.util.Optional;

/**
 * Lets an access policy tell the audit aspect <em>why</em> it granted access. The policy calls
 * {@link #grant(AuthorizationBasis)} inside the audited method; the aspect reads and clears it.
 */
public final class AuthorizationBasisHolder {

  private static final ThreadLocal<AuthorizationBasis> CURRENT = new ThreadLocal<>();

  private AuthorizationBasisHolder() {}

  public static void grant(AuthorizationBasis basis) {
    CURRENT.set(basis);
  }

  public static Optional<AuthorizationBasis> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static void clear() {
    CURRENT.remove();
  }
}
