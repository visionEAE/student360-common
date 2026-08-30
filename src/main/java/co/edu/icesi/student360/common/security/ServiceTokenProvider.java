package co.edu.icesi.student360.common.security;

/**
 * Port: obtains a token proving this service's identity to a target service. Stage 1 adapter signs
 * a JWT locally; stage 2 fetches a Google-signed ID token whose audience is the Cloud Run URL. The
 * callers (gateway filter, Feign interceptors) cannot tell the difference.
 */
public interface ServiceTokenProvider {

  /**
   * Returns a bearer token valid for the given audience — the logical name of the target service,
   * e.g. {@code core-service}.
   */
  String tokenFor(String audience);
}
