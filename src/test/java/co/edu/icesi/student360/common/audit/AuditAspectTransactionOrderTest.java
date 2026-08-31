package co.edu.icesi.student360.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The aspect must run OUTSIDE {@code @Transactional} on the same method: the JDBC writer opens a
 * REQUIRES_NEW transaction, and doing that while the business connection is still checked out needs
 * two connections per request — enough concurrent audited calls then deadlock the pool. This pins
 * the observable contract: by the time the record is written, the business transaction is over.
 */
class AuditAspectTransactionOrderTest {

  @Test
  void shouldWriteTheRecordAfterTheBusinessTransactionHasEnded() {
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(Config.class)) {
      SampleService service = context.getBean(SampleService.class);
      TransactionProbe probe = context.getBean(TransactionProbe.class);

      service.readFinancialStatus("S-1001");

      assertThat(probe.activeDuringBusinessMethod())
          .as("the business method itself runs transactionally")
          .isTrue();
      assertThat(probe.activeDuringAuditWrite())
          .as("the audit record is written once the business transaction has ended")
          .isFalse();
    }
  }

  static class TransactionProbe {
    private volatile boolean activeDuringBusinessMethod;
    private volatile boolean activeDuringAuditWrite;

    boolean activeDuringBusinessMethod() {
      return activeDuringBusinessMethod;
    }

    void activeDuringBusinessMethod(boolean value) {
      activeDuringBusinessMethod = value;
    }

    boolean activeDuringAuditWrite() {
      return activeDuringAuditWrite;
    }

    void activeDuringAuditWrite(boolean value) {
      activeDuringAuditWrite = value;
    }
  }

  static class SampleService {
    private final TransactionProbe probe;

    SampleService(TransactionProbe probe) {
      this.probe = probe;
    }

    @Audited(action = "READ_FINANCIAL_STATUS", subjectType = "STUDENT")
    @Transactional(readOnly = true)
    public String readFinancialStatus(String studentId) {
      probe.activeDuringBusinessMethod(
          TransactionSynchronizationManager.isActualTransactionActive());
      return "status of " + studentId;
    }
  }

  /** Begin/commit bookkeeping only — enough for isActualTransactionActive() to tell the truth. */
  static class ProbeTransactionManager extends AbstractPlatformTransactionManager {
    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(
        Object transaction, org.springframework.transaction.TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }

  @Configuration
  @EnableTransactionManagement
  @EnableAspectJAutoProxy
  static class Config {

    @Bean
    TransactionProbe probe() {
      return new TransactionProbe();
    }

    @Bean
    PlatformTransactionManager transactionManager() {
      return new ProbeTransactionManager();
    }

    @Bean
    AuditTrail auditTrail(TransactionProbe probe) {
      List<AuditRecord> sink = new ArrayList<>();
      AuditWriter writer =
          record -> {
            probe.activeDuringAuditWrite(
                TransactionSynchronizationManager.isActualTransactionActive());
            sink.add(record);
          };
      return new AuditTrail(writer, "test-service", Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    }

    @Bean
    AuditAspect auditAspect(AuditTrail trail) {
      return new AuditAspect(trail);
    }

    @Bean
    SampleService sampleService(TransactionProbe probe) {
      return new SampleService(probe);
    }
  }
}
