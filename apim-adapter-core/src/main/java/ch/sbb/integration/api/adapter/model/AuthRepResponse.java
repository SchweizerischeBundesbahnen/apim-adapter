package ch.sbb.integration.api.adapter.model;

import ch.sbb.integration.api.adapter.service.utils.HttpMethod;

import java.io.Serializable;
import java.util.List;

/**
 * Created by u217269 on 19.02.2018.
 */
public class AuthRepResponse implements Serializable {

    private static final long serialVersionUID = 8619844759446038298L;

    private final boolean allowed;
    private final String clientId;
    private final int httpStatus;
    private final String wwwAuthenticateResponseHeader;
    private final List<String> matchingMetricSysNames;
    private final String message;
    private final String path;
    private final String queryString;
    private final HttpMethod method;


    public AuthRepResponse(boolean allowed, String clientId, int httpStatus, String wwwAuthenticateResponseHeader, List<String> matchingMetricSysNames, String message, String path, String queryString, HttpMethod method) {
        this.allowed = allowed;
        this.clientId = clientId;
        this.httpStatus = httpStatus;
        this.matchingMetricSysNames = matchingMetricSysNames;
        this.message = message;
        this.path = path;
        this.queryString = queryString;
        this.method = method;
        this.wwwAuthenticateResponseHeader = wwwAuthenticateResponseHeader;
    }

    public AuthRepResponse(boolean allowed, String clientId, int httpStatus, List<String> matchingMetricSysNames, String message, String path, String queryString, HttpMethod method) {
        this(allowed, clientId, httpStatus, null, matchingMetricSysNames, message, path, queryString, method);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getClientId() {
        return clientId;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getWwwAuthenticateResponseHeader() {
        return wwwAuthenticateResponseHeader;
    }

    public List<String> getMatchingMetricSysNames() {
        return matchingMetricSysNames;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return "AuthRepResponse{" +
                "allowed=" + allowed +
                ", clientId='" + clientId + '\'' +
                ", httpStatus=" + httpStatus +
                ", matchingMetricSysNames=" + matchingMetricSysNames +
                ", message='" + message + '\'' +
                '}';
    }
}
