package ch.sbb.integration.api.gateway.handler;

import ch.sbb.integration.api.adapter.config.util.check.ConnectionCheck;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import ch.sbb.integration.api.gateway.ApimSingleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.server.Server;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5008;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5009;


public class HealthHandler implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ThreeScaleAdapterHandler.class);

    private final EmergencyModeState emergencyModeState;

    public HealthHandler(EmergencyModeState emergencyModeState) {
        this.emergencyModeState = emergencyModeState;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws JsonProcessingException {
        CheckResult health = new CheckResult("GatewayHealthCheckStatus", Status.UP,
                "Below are the health checks for the gateway");
        
        ConnectionCheck connectionCheck = new ConnectionCheck(ApimSingleton.getAdapterConfig(), emergencyModeState);

        try {
            if (!ApimSingleton.isApimFilterEnabled()){
                health.addCheck(new CheckResult("Apim filter is disabled", Status.UP,
                        "At the moment, the apim filter is disabled. This behavior is only recommended in a " +
                                "emergency and can provide security issues (no check will be made in this operation mode)."));
                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(health));
            }else{
                health.addCheck(connectionCheck.checkURIfor401or403("http://" + Server.config.getIp() + ":" +
                        Server.config.getHttpPort() + "/expectingForbidden4HealthCheck"));
                health.addCheck(ApimSingleton.get().healthCheck());
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                int statuscode = health.isUp() ? HttpStatus.SC_OK : HttpStatus.SC_INTERNAL_SERVER_ERROR;
                if(!health.isUp()){
                    LOG.warn(APIM_5009.pattern(), health);
                }
                exchange.setStatusCode(statuscode);
                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(health));
            }
        }catch (Exception e){
            LOG.warn(APIM_5008.pattern(), e);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(
                            new CheckResult("Exception caught during health check", Status.DOWN, e.getMessage())
            ));
        }
    }
}
