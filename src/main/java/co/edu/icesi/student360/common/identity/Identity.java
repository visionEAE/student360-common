package co.edu.icesi.student360.common.identity;

import java.util.Set;
import java.util.UUID;

/**
 * The end user on whose behalf a request is processed, as asserted by the gateway after it
 * validated the user's access token. {@code externalReference} is the {@code ref} claim: the
 * student or advisor id known to the domain services.
 */
public record Identity(UUID userId, Set<String> roles, String externalReference) {

  public Identity {
    roles = Set.copyOf(roles);
  }

  public boolean hasRole(String role) {
    return roles.contains(role);
  }
}
