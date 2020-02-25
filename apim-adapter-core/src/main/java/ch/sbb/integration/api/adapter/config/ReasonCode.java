package ch.sbb.integration.api.adapter.config;

public enum ReasonCode {

    /**
     * ADAPTER INFO
     */
    APIM_1001("Exception caught while testing url={} exception message={}"),
    APIM_1002("Initialize APIM Adapter Filter with the following excludeFilterMethods={}"),
    APIM_1003("Finished switching to next period on metric={} of client={}, new period start={} end={}"),
    APIM_1004("Finished scheduler with status:"),
    APIM_1005("Exception during deletion of entry in Pushgateway"),
    APIM_1006("Invalidating Token cache with size={} "),
    APIM_1007("Issuer resolved. IssuerURL={}"),
    APIM_1008("Public keys of tokenIssuer={} did not change, token remains invalid"),
    APIM_1009("Public keys of tokenIssuer={} did change, token however remains invalid"),
    APIM_1010("Got a Token with invalid signature, this verifier can't handle it, returning null"),
    APIM_1011("Adding JWK with KeyId={} JwksUri={}"),
    APIM_1012("Invalidating Token cache with size={}"),
    APIM_1013("Exception while loading plan config. Returning SERVER_ERROR response"),
    APIM_1014("Could not find plan for serviceId={} and clientId={} in offline cache"),
    APIM_1015("configType={} changed to newState={}"),
    APIM_1016("Emergency Mode changed to state={}"),
    APIM_1017("Client with the clientId={} was not found in 3scale - httpStatus={} xml={}"),
    APIM_1018("Add unreported hits: {} - {}"),
    APIM_1019("Stopped sync of this client because the client was not found in 3scale clientId={}"),
    APIM_1020("Removed client from the cache clientId={}"),
    APIM_1021("Stopped sync of this client sync state was unknown clientId={}"),
    APIM_1022("Monitoring is disabled"),
    APIM_1023("Monitoring push enabled"),
    APIM_1024("Reporting {} hits to 3scale"),
    APIM_1025("Error from 3scale while reporting: httpStatus={} reason={}"),
    APIM_1026("Starting graceful shutdown"),
    APIM_1027("Client was allowed but responding to client with: httpStatus={} request: method={} path={} queryString={} matchingMetric={}"),
    APIM_1028("Added new Metric {} while syncing with 3scale"),
    APIM_1029("Found the following manifestList={}"),
    APIM_1030("Failed to push to prometheus! host={}"),
    APIM_1031("Shutting down MonitoringService"),
    APIM_1032("Using cache location={}"),
    APIM_1033("Handled invalid request with: clientId={} httpStatus={} reason={} path={} method={}"),
    APIM_1034("JWKS loaded in duration={} ms JwksUri={}"),

    /**
     * ADAPTER WARNING
     */
    APIM_2001("Removed this client from the cache because the sync state was unknown clientId={}"),
    APIM_2002("Empty response when reading current client stats from: clientId={} statusCode={}"),
    APIM_2003("Couldn't load sso public key, still not ready"),
    APIM_2004("The 3Scale sync was not running for {} seconds. syncRateInSeconds={}"),
    APIM_2005("Missing environment parameter={} | switching to default file contained in jarResource={}"),
    APIM_2006("Monitoring Level is not in MonitoringLevel Enum using STANDARD level"),
    APIM_2007("APIM Filter is disabled!"),
    APIM_2008("Failed to update cache value configType={} use oldValue={}"),
    APIM_2009("Got an httpStatus={} with reason={} when loading Proxy settings from 3Scale backendUrl={} "),
    APIM_2010("Exception during deletion of entry in Pushgateway"),
    APIM_2011("Could not load plan for client={} and service={} with response={}"),
    APIM_2012("Could not load plan for service={} and client={} from threeScaleAdminCommunicationComponent. Try to load it from offline cache"),
    APIM_2013("Could not load MappingRules for service={} from threeScaleAdminCommunicationComponent: ExceptionMessage={}. Try to load it from offline cache"),
    APIM_2014("Could not load Metrics for service={} from threeScaleAdminCommunicationComponent: ExceptionMessage={}. Try to load it from offline cache"),
    APIM_2015("Could not load Proxy Config for service={} from threeScaleAdminCommunicationComponent:  ExceptionMessage={}. Try to load it from offline cache"),
    APIM_2016("Could not open Manifests."),
    APIM_2017("Could not load Manifests."),
    APIM_2018("Could not find manifest with {}: {}"),
    APIM_2019("Unable to create JMX Collector"),
    APIM_2020("Unable to create Pushgateway URL from config"),
    APIM_2021("Failed to delete all cache files with baseFileName={}"),
    APIM_2022("Failed to iterate over existing files in order to perform file rotation. baseFileName={}"),
    APIM_2023("Delete failed file={}"),
    APIM_2024("Could not load JwksUri={} after duration={} ms. Got statusCode={} ."),
    APIM_2025("Error loading JWKS from token issuer url={}"),
    APIM_2026("Loaded mapping rules with httpStatus={} duration={} ms"),
    APIM_2027("Empty response when reading proxy config url={} httpStatus={} message={}"),
    APIM_2028("Exception while reporting duration={} ms"),
    APIM_2029("Got exception while loading plan in 3scale for clientId={} duration={} ms"),
    APIM_2030("Encountered unresolvable / untrusted Issuer. IssuerURL={}"),
    APIM_2031("Unable to parse '{}' as HttpMethod"),
    APIM_2032("Unable to terminate Monitoring Scheduler"),
    APIM_2033("Path or method was null: path={} method={}"),
    APIM_2034("Client id was null or empty"),
    APIM_2035("Token validation failed because ParsedToken is null, returning {}"),
    APIM_2036("Public Key was invalid KeyId={} JwksUri={}"),
    APIM_2037("Public Key not found with KeyId={} JwksUri={}"),
    APIM_2038("Could not load OidcUrl={} after duration={} ms. Got statusCode={} . Try to load it from offline cache"),
    APIM_2039("Removing JWK with KeyId={} JwksUri={}"),
    APIM_2040("Error during reloading of JWKS. Pause for at least one minute before next attempt"),
    APIM_2041("Unable to parse ClientId from Token: {}"),
    APIM_2042("{} Try to load it from offline cache"),

    /**
     * ADAPTER ERROR
     */

    APIM_3001("Error in Batch Sync with 3Scale."),
    APIM_3002("Couldn't load target uri from cache, becoming unhealthy:"),
    APIM_3003("Couldn't setup ResteasyClient"),
    APIM_3004("Usage for metric={} in client={} was null"),
    APIM_3005("Error when loading the initial client usage stats for serviceId={} and clientId={}"),
    APIM_3006("Unable to generate public key verifier"),
    APIM_3007("Unable to eagerly initialize TokenIssuer. IssuerURL={}"),
    APIM_3008("Error Reading Metrics Response"),
    APIM_3009("Error when loading the initial client usage Stats"),
    APIM_3010("Unable to initialize OfflineConfigurationCacheRepo"),
    APIM_3011("Exception while trying to find latest cached config baseFileName={}"),
    APIM_3012("No latest config found for baseFileName={}"),
    APIM_3013("Failed to persist new config file with baseFileName={}"),
    APIM_3014("Unable to load Metrics httpStatus={} with url={}"),
    APIM_3015("Unable to load proxy from 3scale!"),
    APIM_3016("Token must not be null"),
    APIM_3017("Unable to Synchronize with 3scale before shutdown"),
    APIM_3018("Error Reading ProxyConfig Response"),
    APIM_3019("Error at loading config from config file={}"),
    APIM_3020("Error at loading config property={} from config file={} - nodeName={} does not exist."),
    APIM_3021("Error Reading MappingRules Response"),
    APIM_3022("Got an empty JWKS, NOT updating the TokenIssuers JWKS store! A configured Token Issuer should at least return one JWK in the JWKS JwksUri={}"),
    APIM_3023("Unknown error, not caching the token and returning internal server error"),
    APIM_3024("Error at loading property={} from config file={} - nodeName={} was unexpectedly an array"),
    APIM_3025("Error at loading property={} from config file={} - nodeName={} was expected to be an array node, but was not"),

    /**
     * GATEWAY INFO
     */

    APIM_4001("Req/Res message={} returnedStatusCode={} TotalDuration={} ms ApimDuration={} ms ProxyDuration={} ms proxyRequestStatus={} clientId={} clientAllowed={} method={} path={} queryString={}"),
    APIM_4002("Starting undertow server for admin endpoints ip={} port={}"),
    APIM_4003("Shutdown server because /shutdown was called."),
    APIM_4004("Gateway is ready"),
    APIM_4005("Using the following correlationId / Business ID headers correlation={}, business={}"),
    APIM_4006("ProxyCallback message={} connectionTime={} ms, method={} path={} queryString={}"),

    /**
     * GATEWAY WARNING
     */

    APIM_5001("No matching targetHostBasePathUri could be found"),
    APIM_5002("Overriding targetUri with proxy from cache {} because targetHostBasePathUri is null"),
    APIM_5003("Ignoring rule as location is not set"),
    APIM_5004("Readiness check failed with the following error: {}"),
    APIM_5005("Exception caught during readiness check"),
    APIM_5006("Unable to write in Prometheus format"),
    APIM_5007("Req/Res message={} returnedStatusCode={} TotalDuration={} ms ApimDuration={} ms ProxyDuration={} ms proxyRequestStatus={} clientId={} clientAllowed={} method={} path={} queryString={}"),
    APIM_5008("Exception caught during health check"),
    APIM_5009("Health check failed with the following error: {}"),
    APIM_5010("MDC was not cleared before, check if we somewhere forgot to clear the MDC before."),
    APIM_5011("ProxyCallback message={} connectionTime={} ms, method={} path={} queryString={}"),

    /**
     * GATEWAY ERROR
     */

    APIM_6001("Parameter proxyConfig is null"),
    APIM_6002("Couldn't parse loaded Proxy from Apim as URI:"),
    APIM_6003("TargetHostBasePathUri is still null after all recovery attempts, returning 503"),
    APIM_6004("Could not create URI from cached url"),
    APIM_6005("Proxy error state is: targetHostBasePathUri={}, rules={}, defaultUri={}, requestLocation={}"),
    APIM_6006("Internal Server Error while handling request"),
    APIM_6007("HTTP Method was null"),
    APIM_6008("Unable to parse '{}' as HttpMethod");


    private final int code;
    private final String pattern;
    private final String stringFormat;

    ReasonCode(String logString) {
        this.code = Integer.parseInt(name().replace("APIM_", ""));
        this.pattern = "APIM-" + this.code + ": " + logString;
        this.stringFormat = pattern.replaceAll("\\{}", "%s");
    }

    public int getCode() {
        return code;
    }

    public String pattern() {
        return pattern;
    }

    String stringFormat() {
        return stringFormat;
    }

    public String format(Object... args) {
        return String.format(stringFormat, args);
    }

    @Override
    public String toString() {
        return pattern;
    }

}
