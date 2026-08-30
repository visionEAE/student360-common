package co.edu.icesi.student360.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.icesi.student360.common.identity.IdentityHeaders;
import co.edu.icesi.student360.common.logging.Correlation;
import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import co.edu.icesi.student360.common.testapp.ProbeApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Phase gate 0.B: a throwaway service using the library emits JSON logs carrying the request id,
 * refuses calls without a service token, and leaves audit and outbox rows in a real PostgreSQL.
 */
@SpringBootTest(
    classes = ProbeApplication.class,
    properties = {
      "student360.service-name=probe-service",
      "student360.security.service-token.secret=0123456789abcdef0123456789abcdef-test-only",
      "logging.level.root=INFO"
    })
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class FoundationsIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16").withInitScript("db/test-init.sql");

  private static final UUID STUDENT_USER = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ServiceTokenProvider tokens;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void cleanTables() {
    jdbc.update("DELETE FROM audit.audit_record");
    jdbc.update("DELETE FROM outbox_event");
  }

  @Test
  void shouldRejectCallWithoutServiceTokenBeforeReachingTheDomain() throws Exception {
    mockMvc
        .perform(get("/api/probe/S-1001").header(IdentityHeaders.USER_ID, STUDENT_USER))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Authentication failed"))
        .andExpect(jsonPath("$.requestId").isString());

    assertThat(count("audit.audit_record")).isZero();
  }

  @Test
  void shouldAuditAllowedAccessWriteOutboxAndCorrelateLogs(CapturedOutput output) throws Exception {
    String requestId = "gate-0b-" + UUID.randomUUID();

    mockMvc
        .perform(asStudent(get("/api/probe/S-1001"), requestId))
        .andExpect(status().isOk())
        .andExpect(header().string(Correlation.REQUEST_ID_HEADER, requestId))
        .andExpect(jsonPath("$.studentId").value("S-1001"));

    Map<String, Object> audit = single("audit.audit_record");
    assertThat(audit)
        .containsEntry("request_id", requestId)
        .containsEntry("service_name", "probe-service")
        .containsEntry("record_type", "DATA_ACCESS")
        .containsEntry("action", "READ_PROBE")
        .containsEntry("subject_type", "STUDENT")
        .containsEntry("subject_id", "S-1001")
        .containsEntry("authorization_basis", "SELF")
        .containsEntry("outcome", "ALLOWED")
        .containsEntry("actor_id", STUDENT_USER);
    assertThat(audit.get("trace_id")).as("trace id from micrometer tracing").isNotNull();

    Map<String, Object> outbox = single("outbox_event");
    assertThat(outbox)
        .containsEntry("event_type", "PROBE_READ")
        .containsEntry("aggregate_id", "S-1001");
    JsonNode payload = objectMapper.readTree(outbox.get("payload").toString());
    assertThat(payload.path("requestId").asText()).isEqualTo(requestId);
    assertThat(payload.path("traceId").asText()).isEqualTo(audit.get("trace_id"));
    assertThat(payload.path("data").path("ok").asBoolean()).isTrue();

    assertThat(output.getOut())
        .as("JSON log line carrying the request id")
        .contains("\"message\":\"Probe read for S-1001\"")
        .contains("\"requestId\":\"" + requestId + "\"")
        .contains("\"service\":\"probe-service\"")
        .contains("\"traceId\":\"");
  }

  @Test
  void shouldAuditDeniedAccessAndAnswer403WithoutRollingBackTheRecord() throws Exception {
    String requestId = "gate-0b-denied-" + UUID.randomUUID();

    mockMvc
        .perform(asStudent(get("/api/probe/S-9999"), requestId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access denied"))
        .andExpect(jsonPath("$.requestId").value(requestId));

    Map<String, Object> audit = single("audit.audit_record");
    assertThat(audit)
        .containsEntry("request_id", requestId)
        .containsEntry("outcome", "DENIED")
        .containsEntry("authorization_basis", "NONE")
        .containsEntry("subject_id", "S-9999");
    assertThat(count("outbox_event")).as("business transaction rolled back").isZero();
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asStudent(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      String requestId) {
    return request
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.tokenFor("probe-service"))
        .header(Correlation.REQUEST_ID_HEADER, requestId)
        .header(IdentityHeaders.USER_ID, STUDENT_USER.toString())
        .header(IdentityHeaders.USER_ROLES, "STUDENT")
        .header(IdentityHeaders.EXTERNAL_REFERENCE, "S-1001");
  }

  private Map<String, Object> single(String table) {
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table);
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }

  private int count(String table) {
    Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    return count == null ? 0 : count;
  }
}
