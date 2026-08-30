package co.edu.icesi.student360.common.logging;

/** MDC keys shared by the correlation and identity filters and the JSON log encoder. */
public final class MdcKeys {

  public static final String REQUEST_ID = "requestId";
  public static final String USER_ID = "userId";

  /** Populated by Micrometer Tracing; listed here so the encoder configuration has one source. */
  public static final String TRACE_ID = "traceId";

  public static final String SPAN_ID = "spanId";

  private MdcKeys() {}
}
