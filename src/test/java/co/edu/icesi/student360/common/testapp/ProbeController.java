package co.edu.icesi.student360.common.testapp;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

  private static final Logger log = LoggerFactory.getLogger(ProbeController.class);
  private final ProbeService service;

  public ProbeController(ProbeService service) {
    this.service = service;
  }

  @GetMapping("/api/probe/{studentId}")
  public Map<String, String> read(@PathVariable String studentId) {
    log.info("Probe read for {}", studentId);
    return service.read(studentId);
  }
}
