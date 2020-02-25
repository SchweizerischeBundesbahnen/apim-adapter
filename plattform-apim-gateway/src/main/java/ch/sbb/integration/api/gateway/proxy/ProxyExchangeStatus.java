package ch.sbb.integration.api.gateway.proxy;

public enum ProxyExchangeStatus {
    COMPLETED,
    FAILED,
    QUEUED_REQUEST_FAILED,
    COULD_NOT_RESOLVE_BACKEND;
}
