package co.edu.icesi.student360.common.logging;

import java.util.Optional;
import org.slf4j.MDC;

/** Read access to the correlation identifiers of the request being processed on this thread. */
public final class Correlation {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";

  private Correlation() {}

  public static Optional<String> currentRequestId() {
    return Optional.ofNullable(MDC.get(MdcKeys.REQUEST_ID));
  }

  public static Optional<String> currentTraceId() {
    return Optional.ofNullable(MDC.get(MdcKeys.TRACE_ID));
  }
}
