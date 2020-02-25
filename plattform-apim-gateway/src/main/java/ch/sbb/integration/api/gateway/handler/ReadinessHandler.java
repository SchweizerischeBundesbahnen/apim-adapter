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

import java.io.IOException;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_4004;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5004;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5005;


public class ReadinessHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReadinessHandler.class);

    private final EmergencyModeState emergencyModeState;
    private boolean ready;
    private boolean shuttingDown;

    public ReadinessHandler(EmergencyModeState emergencyModeState) {
        this.emergencyModeState = emergencyModeState;
        this.shuttingDown = false;
        if (!ApimSingleton.isApimFilterEnabled()){
            this.ready = true;
        }
    }

    /**
     * Openshift calls the readiness check all the time but here in the checks are some startup checks which
     * aren't required during the runtime
     * so we need the check until the app is ready and afterwards we check if the application has started and return
     * this result. The health/liviness will be checked with the healthcheck
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws IOException {

        if (shuttingDown) {
            final CheckResult shuttingDownCheck = new CheckResult("ShuttingDownCheck ", Status.DOWN,
                    "This check overrides all the other checks, " +
                            "because the system is shutting down and has to be unready!");

            exchange.setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(shuttingDownCheck));
        } else {
            final CheckResult applicationStartedCheck = new CheckResult("ApplicationStartedCheck", Status.UP,
                    "This check overrides all the other checks, " +
                            "because the system is running. For health check out /health!");

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            if (this.ready) {
                exchange.setStatusCode(HttpStatus.SC_OK);
                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(applicationStartedCheck));
            } else {
                this.ready = evaluateReadiness(exchange, applicationStartedCheck);
            }
        }
    }

    public void shutDown() {
        this.shuttingDown = true;
    }

    private boolean evaluateReadiness(final HttpServerExchange exchange, final CheckResult applicationStartedCheck) throws JsonProcessingException {
        try {
            final CheckResult check = new CheckResult("GatewayReadinessCheck", Status.UP,
                    "Below are the checks for the readiness of the gateway listed");

            final ConnectionCheck connectionCheck = new ConnectionCheck(ApimSingleton.getAdapterConfig(), emergencyModeState);
            check.addCheck(connectionCheck.checkURIfor401or403(String.format("http://%s:%d/expectingForbidden4HealthCheck", Server.config.getIp(), Server.config.getHttpPort())));

            check.addCheck(ApimSingleton.get().readinessCheck());

            if (check.isUp()) {
                exchange.setStatusCode(HttpStatus.SC_OK);
                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(applicationStartedCheck));
                // this is the first time the readiness check is successful, so we set ready to true
                LOG.info(APIM_4004.pattern());
                return true;
            } else {
                LOG.warn(APIM_5004.pattern(), check);
                exchange.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(check));
            }
        }catch (Exception e){
            LOG.warn(APIM_5005.pattern(), e);
            exchange.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(
                    new CheckResult("Exception caught during readiness check", Status.DOWN, e.getMessage())
            ));
        }
        return false;
    }
}
