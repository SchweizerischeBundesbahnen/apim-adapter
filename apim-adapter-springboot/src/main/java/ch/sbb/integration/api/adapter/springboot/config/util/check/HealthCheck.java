package ch.sbb.integration.api.adapter.springboot.config.util.check;

import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.service.ApimAdapterService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class HealthCheck implements HealthIndicator {

	private ApimAdapterService service;
	
	private boolean ready;
	
	public HealthCheck(ApimAdapterService service) {
		this.service = service;
	}
	
	public Health health() {
		if(!ready) {
			CheckResult result = service.readinessCheck();
			ready = result.isUp();
			return buildHealth(result);
		}
		
		return buildHealth(service.healthCheck());
	}
	
	private Health buildHealth(CheckResult result) {
		return (result.isUp() ? Health.up()
				.withDetail(result.getName(), result)
				.build() : Health.down()
				.withDetail(result.getName(), result)
				.build()
			);	
	}
	
}
