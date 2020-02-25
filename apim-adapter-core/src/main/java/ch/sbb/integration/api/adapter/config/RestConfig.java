package ch.sbb.integration.api.adapter.config;


import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_3003;

/**
 * Created by u217269 on 16.02.2018.
 */
public class RestConfig {
    private static final Logger LOG = LoggerFactory.getLogger(RestConfig.class);
    private static final int PRODUCTION_READ_TIMEOUT_IN_MILLIS = 30_000;
    private static final int PRODUCTION_CONNECT_TIMEOUT_IN_MILLIS = 5_000;

    private final int connectTimeoutInMillis;
    private final int readTimeoutInMillis;

    public RestConfig() {
        this(PRODUCTION_CONNECT_TIMEOUT_IN_MILLIS, PRODUCTION_READ_TIMEOUT_IN_MILLIS);
    }

    public RestConfig(int connectTimeoutInMillis, int readTimeoutInMillis) {
        this.connectTimeoutInMillis = connectTimeoutInMillis;
        this.readTimeoutInMillis = readTimeoutInMillis;
    }

    public ResteasyClient newRestEasyClient() {
        try {
            LOG.debug("Setting connect timeout to {} ms and read timeout to {} ms", connectTimeoutInMillis, readTimeoutInMillis);
            final RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(connectTimeoutInMillis)
                    .setConnectionRequestTimeout(connectTimeoutInMillis)
                    .setSocketTimeout(readTimeoutInMillis)
                    .build();

            final HttpClient httpClient = HttpClientBuilder.create().useSystemProperties().setDefaultRequestConfig(requestConfig).build();

            return new ResteasyClientBuilder()
                    .httpEngine(new ApacheHttpClient4Engine(httpClient))
                    .build();
        } catch (Exception e) {
            LOG.error(APIM_3003.pattern(), e);
            throw new RuntimeException("Error creating ResteasyClient.", e);
        }
    }

}
