package ch.sbb.integration.api.adapter.service.apiwatch;

public class ApiWatchResponse {
    private String version;
    private String operationMode;
    private String cacheLocation;
    private String offlineConfigurationCache;
    private String backendResponse;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(String operationMode) {
        this.operationMode = operationMode;
    }

    public String getCacheLocation() {
        return cacheLocation;
    }

    public void setCacheLocation(String cacheLocation) {
        this.cacheLocation = cacheLocation;
    }

    public String getOfflineConfigurationCache() {
        return offlineConfigurationCache;
    }

    public void setOfflineConfigurationCache(String offlineConfigurationCache) {
        this.offlineConfigurationCache = offlineConfigurationCache;
    }

    public String getBackendResponse() {
        return backendResponse;
    }

    public void setBackendResponse(String backendResponse) {
        this.backendResponse = backendResponse;
    }
}
