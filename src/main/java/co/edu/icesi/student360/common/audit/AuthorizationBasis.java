package co.edu.icesi.student360.common.audit;

/**
 * The relationship that justified an access. Recording it is what lets the trail answer "why was
 * this advisor allowed to see this student" months later, instead of merely "that they did".
 */
public enum AuthorizationBasis {
  /** The subject is the caller themself (the {@code ref} claim equals the requested id). */
  SELF,
  /** An active advisor assignment linked the caller to the student. */
  ASSIGNMENT,
  /** The caller holds the ADMIN role, which grants access regardless of relationship. */
  ADMIN_ROLE,
  /**
   * The caller is staff (an advisor) reading institutional records. Whether the advisor is actually
   * assigned to the student is enforced by support-service, which owns assignments.
   */
  STAFF_ROLE,
  /** A sibling service acting on its own behalf. */
  SERVICE,
  /** No relationship was found; the only valid basis for a denied outcome. */
  NONE
}
