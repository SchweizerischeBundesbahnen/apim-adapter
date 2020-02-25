package ch.sbb.integration.api.adapter.springboot2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class RunSpring2Adapter {
    public static void main(String[] args) {
        SpringApplication.run(RunSpring2Adapter.class, args);
    }

    /**
     * Default endpoint for manual testing.
     */
    @RestController
    public class Controller {
        @RequestMapping("/api/")
        public String greeting() {
            return "hello";
        }
    }

}

