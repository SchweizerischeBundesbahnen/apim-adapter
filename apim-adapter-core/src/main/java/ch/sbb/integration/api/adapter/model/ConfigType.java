package ch.sbb.integration.api.adapter.model;

/**
 * This enum maps to the types of configs read from remote API management
 */
public enum ConfigType {
    @Deprecated
    TOKEN_ISSUER_PUBLIC_KEY("token-issuer-public-key", false),
    OIDC("oidc", true),
    JWKS("jwks", true),
    METRIC("metric", true),
    PROXY("proxy", true),
    PLAN("plan", true),
    MAPPING_RULES("mapping-rules", true);
    private final String id;
    private final boolean appendBasename;

    ConfigType(String id, boolean appendBasename) {
        this.id = id;
        this.appendBasename = appendBasename;
    }

    public String getId() {
        return id;
    }

    public boolean isAppendBasename() {
        return appendBasename;
    }

    @Override
    public String toString() {
        return id;
    }
}
