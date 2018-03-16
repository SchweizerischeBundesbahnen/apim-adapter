package ch.sbb.integration.api.threescale.config;


import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * Created by u217269 on 16.02.2018.
 */
public class RestConfig {

    private static final Logger LOG = Logger.getLogger(RestConfig.class);

    public static ResteasyClient newRestEasyClient() {
        try {

            setDefaultTlsTruststoreForAllHttpClients();

            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(loadTrustStore(), TrustSelfSignedStrategy.INSTANCE).build();
            ConnectionSocketFactory trustedOnly = new SSLConnectionSocketFactory(sslContext, new DefaultHostnameVerifier());
            ConnectionSocketFactory openForAll = PlainConnectionSocketFactory.INSTANCE;
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", trustedOnly)
                    .register("http", openForAll)
                    .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            HttpClient httpClient = HttpClients.createMinimal(cm);
            ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);

            return new ResteasyClientBuilder()
                    .httpEngine(engine)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error creating RestEasyClient.", e);
        }
    }

    private static void setDefaultTlsTruststoreForAllHttpClients() {
        TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = loadTrustStoreAsStream();
            keystore.load(keystoreStream, null);
            trustManagerFactory.init(keystore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustManagers, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Throwable t) {
            LOG.error("Unable to load and set TLS Truststore.", t);
        }
    }

    private static KeyStore loadTrustStore() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("jks");
        InputStream inputStream = loadTrustStoreAsStream();
        trustStore.load(inputStream, null);
        return trustStore;
    }

    private static InputStream loadTrustStoreAsStream() throws Exception {
        String pathToTruststore = ThreeScaleConfig.truststore();
        LOG.info("Loading truststore from: " + String.valueOf(pathToTruststore));

        InputStream truststoreStream;
        File file = new File(pathToTruststore);
        if (file != null && file.exists() && file.isAbsolute()) {
            truststoreStream = new FileInputStream(file);
        } else {
            LOG.warn("Truststore cannot be load: " + pathToTruststore + " | switching to default truststore contained in jar-resources: truststore.jks");
            truststoreStream = ResteasyClient.class.getResourceAsStream("/truststore.jks");
        }
        return truststoreStream;
    }

}
