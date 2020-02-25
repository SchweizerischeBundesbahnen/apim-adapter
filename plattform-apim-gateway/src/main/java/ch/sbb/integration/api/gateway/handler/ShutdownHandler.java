package ch.sbb.integration.api.gateway.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_4003;


public class ShutdownHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownHandler.class);

    private final ReadinessHandler readinessHandler;

    public ShutdownHandler(ReadinessHandler readinessHandler) {
        this.readinessHandler = readinessHandler;
    }

    /**
     * To enable a graceful shutdown on OpenShift, it is necessary to make the pod unready and wait the readiness check
     * interval before you can shutdown the server. This handler will be called from the preStop hook of OpenShift.
     *
     * It sets the ReadynessHandler to unhealthy. So the OpenShift HA router will not rout further requests to the
     * pod. And the oder ShutdownHooks can cleanup the rest.
     *
      * @param exchange
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) {
        LOG.info(APIM_4003.pattern());
        readinessHandler.shutDown();
    }
}
