package co.edu.icesi.student360.common.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Defaults every service would otherwise have to repeat. Registered with the lowest precedence so
 * any {@code application.yml} or environment variable overrides them.
 *
 * <p>Sampling is 100% because no exporter runs in stage 1 and the ids exist only to correlate log
 * lines; stage 2 lowers it when a collector is attached.
 */
public class TracingDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String SOURCE_NAME = "student360-defaults";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    environment
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                SOURCE_NAME,
                Map.of(
                    "management.tracing.sampling.probability", "1.0",
                    "management.tracing.propagation.type", "w3c",
                    "spring.jpa.open-in-view", "false",
                    "spring.jackson.default-property-inclusion", "non_null")));
  }
}
