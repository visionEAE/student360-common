package co.edu.icesi.student360.common.identity;

/** Headers through which the gateway propagates the validated user identity downstream. */
public final class IdentityHeaders {

  public static final String USER_ID = "X-User-Id";
  public static final String USER_ROLES = "X-User-Roles";
  public static final String EXTERNAL_REFERENCE = "X-External-Reference";

  private IdentityHeaders() {}
}
