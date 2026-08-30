package co.edu.icesi.student360.common.audit;

/** Which consumer a record is for: data-access reviews, security monitoring, or change history. */
public enum RecordType {
  DATA_ACCESS,
  SECURITY,
  STATE_CHANGE
}
