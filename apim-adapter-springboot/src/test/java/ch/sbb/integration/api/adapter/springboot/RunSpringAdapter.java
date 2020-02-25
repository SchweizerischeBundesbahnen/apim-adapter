package ch.sbb.integration.api.adapter.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Helper class to run Adapter with spring locally.
 * The following configuration has to be set:
 * <ul>
 * <li>apim.admin.token</li>
 * <li>apim.backend.token</li>
 * <li>apim.adapter.service-id</li>
 * <li>apim.monitoring.namespace</li>
 * <li>apim.monitoring.id</li>
 * </ul>
 */
@SpringBootApplication
public class RunSpringAdapter {
    public static void main(String[] args) {
        SpringApplication.run(RunSpringAdapter.class, args);
    }
}

