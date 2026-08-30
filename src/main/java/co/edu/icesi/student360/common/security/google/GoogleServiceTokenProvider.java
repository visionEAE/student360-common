package co.edu.icesi.student360.common.security.google;

import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage 2 adapter: mints Google-signed ID tokens for private Cloud Run callees. The contract is
 * unchanged — callers still say {@code tokenFor("core-service")} — and the logical name is resolved
 * to the callee's URL through the audience map, which production feeds from the same {@code
 * *_SERVICE_URL} variables the HTTP clients use, so the token's audience and the request's
 * destination can never disagree.
 *
 * <p>The underlying credentials come from Application Default Credentials — on Cloud Run, the
 * service's own runtime service account via the metadata server. {@link IdTokenCredentials} caches
 * and refreshes the token internally (roughly hourly), so only the first call per audience blocks
 * on the metadata server; acceptable in the gateway's reactive path at this scale, noted rather
 * than hidden.
 */
public class GoogleServiceTokenProvider implements ServiceTokenProvider {

  private final IdTokenProvider identity;
  private final Map<String, String> audienceMap;
  private final ConcurrentHashMap<String, IdTokenCredentials> credentialsByAudience =
      new ConcurrentHashMap<>();

  public GoogleServiceTokenProvider(IdTokenProvider identity, Map<String, String> audienceMap) {
    this.identity = identity;
    this.audienceMap = Map.copyOf(audienceMap);
  }

  @Override
  public String tokenFor(String audience) {
    String url = audienceMap.get(audience);
    if (url == null || url.isBlank()) {
      throw new IllegalStateException(
          "No URL configured for service '"
              + audience
              + "' — add it to student360.security.service-token.audience-map");
    }
    IdTokenCredentials credentials =
        credentialsByAudience.computeIfAbsent(
            audience,
            key ->
                IdTokenCredentials.newBuilder()
                    .setIdTokenProvider(identity)
                    .setTargetAudience(url)
                    .build());
    try {
      credentials.refreshIfExpired();
      return credentials.getIdToken().getTokenValue();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not obtain a Google ID token for " + audience, exception);
    }
  }
}
