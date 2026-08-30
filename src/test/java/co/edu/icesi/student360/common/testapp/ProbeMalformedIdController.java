package co.edu.icesi.student360.common.testapp;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeMalformedIdController {

  @GetMapping("/api/probe/by-id/{id}")
  public String byId(@PathVariable UUID id) {
    return id.toString();
  }
}
