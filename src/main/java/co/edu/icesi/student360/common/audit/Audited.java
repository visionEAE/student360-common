package co.edu.icesi.student360.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose execution must leave an audit record, allowed or denied. The access
 * policy is expected to run <em>inside</em> the annotated method so that a denial surfaces as an
 * {@code AccessDeniedForSubjectException} the aspect can observe.
 *
 * <pre>{@code
 * @Audited(action = "READ_FINANCIAL_STATUS", subjectType = "STUDENT")
 * public FinancialStatus findFinancialStatus(String studentId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

  /** Upper snake case verb phrase, e.g. {@code READ_FINANCIAL_STATUS}. */
  String action();

  /** What the action is about: {@code STUDENT}, {@code ALERT}, {@code SESSION}. */
  String subjectType();

  RecordType recordType() default RecordType.DATA_ACCESS;

  /** Name of the parameter holding the subject id; defaults to the first parameter. */
  String subjectIdParameter() default "";
}
