package ch.sbb.integration.api.gateway.handler;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_5006;


public class PrometheusHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusHandler.class);

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws IOException {

        Enumeration<Collector.MetricFamilySamples> values = CollectorRegistry.defaultRegistry.metricFamilySamples();
        Writer writer = new StringWriter();
        try {
            TextFormat.write004(writer, values);
            writer.flush();
        } catch (Exception e) {
            LOG.warn(APIM_5006.pattern(), e);
        } finally {
            writer.close();
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
        exchange.setStatusCode(HttpStatus.SC_OK);

        exchange.getResponseSender().send(writer.toString());
    }
}
