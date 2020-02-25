package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.service.configuration.EmergencyModeState;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_1001;

public class ConnectionCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionCheck.class);
    private static final int CONNECTION_TIMEOUT = 6_000;
    private static final int READ_TIMEOUT = 5_000;

    private final ApimAdapterConfig config;
    private final int connectionTimeout;
    private final int readTimeout;
    private final EmergencyModeState emergencyModeState;

    ConnectionCheck(ApimAdapterConfig config, EmergencyModeState emergencyModeState, int connectionTimeoutMillis, int readTimeoutMillis) {
        this.config = config;
        this.emergencyModeState = emergencyModeState;
        this.connectionTimeout = connectionTimeoutMillis;
        this.readTimeout = readTimeoutMillis;
    }

    public ConnectionCheck(ApimAdapterConfig config, EmergencyModeState emergencyModeState) {
    	this(config, emergencyModeState, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }
    
    public CheckResult checkURIfor401or403(String uri) {
        HttpURLConnection conn = null;
        final String checkName = "checkURIfor401or403" + " " + uri;
        try {
            conn = createConnection(uri);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpStatus.SC_FORBIDDEN || responseCode == HttpStatus.SC_UNAUTHORIZED) {
                return new CheckResult(checkName, Status.UP, "expecting to get 401/403! " + createMessageFromConn(conn));
            } else {
                return new CheckResult(checkName, Status.DOWN, "expecting to get 401/403! " + createMessageFromConn(conn));
            }
        } catch (IOException e) {
            LOG.info(APIM_1001.pattern(), uri, e);
            return new CheckResult(checkName, Status.DOWN, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public CheckResult checkURI(String uri) {
        final String notSensitiveUri = uri
                .replace(config.getAdminToken(), "__APIM_ADMIN_TOKEN___")
                .replace(config.getBackendToken(), "___APIM_BACKEND_TOKEN___");
        final String checkName = "checkURI" + " " + notSensitiveUri;

        if(emergencyModeState.isEmergencyMode()) {
            return new CheckResult(checkName, Status.UP, "unchecked - emergency mode");
        }

        HttpURLConnection conn = null;
        try {
            conn = createConnection(uri);

            final int responseCode = conn.getResponseCode();
            if (Response.Status.Family.familyOf(responseCode) == Response.Status.Family.SUCCESSFUL) {
                return new CheckResult(checkName, Status.UP, createMessageFromConn(conn));
            } else {
                return new CheckResult(checkName, Status.DOWN, createMessageFromConn(conn));
            }
        } catch (IOException e) {
            LOG.info(APIM_1001.pattern(), notSensitiveUri, e);
            return new CheckResult(checkName, Status.DOWN, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private HttpURLConnection createConnection(final String uri) throws IOException {
        final URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(connectionTimeout);
        conn.setReadTimeout(readTimeout);
        return conn;
    }

    private static String createMessageFromConn(HttpURLConnection conn) throws IOException {
        return "message: '" + conn.getResponseMessage() + "' HttpStatusCode: '" + conn.getResponseCode() + "'";
    }

    public CheckResult checkThreeScaleAdmin() {
        String protocolThreescale = config.isAdminUseHttps() ? "https" : "http";
        String threeScaleUrl = String.format("%s://%s/admin/api/applications.xml?access_token=%s&page=1&per_page=1", protocolThreescale, config.getAdminHost(), config.getAdminToken());
        return checkURI(threeScaleUrl);
    }

    public CheckResult checkThreeScaleBackend() {
        String protocol = config.isBackendUseHttps() ? "https" : "http";
        String backendUrl = String.format("%s://%s/status", protocol, config.getBackendHost());
        return checkURI(backendUrl);
    }

}
