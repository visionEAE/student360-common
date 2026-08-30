package co.edu.icesi.student360.common.audit;

/**
 * Port: persists one audit record. The stage 1 adapter inserts into the shared append-only table; a
 * stage 2 adapter may additionally export to Cloud Storage with bucket lock.
 */
public interface AuditWriter {

  void write(AuditRecord record);
}
