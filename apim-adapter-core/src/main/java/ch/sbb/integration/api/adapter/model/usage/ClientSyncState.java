package ch.sbb.integration.api.adapter.model.usage;

import java.util.EnumSet;
import java.util.Set;

public enum ClientSyncState {
    /**
     *
     */
    OK,
    /**
     * The application/client (3-scale terminology) was not found on API Management
     */
    APPLICATION_NOT_FOUND,
    /**
     * The usage limits according the the applications/clients plan has exceeded
     */
    USAGE_LIMITS_EXCEEDED,
    /**
     * An unknown, server side failure occurred while accessing API Management
     */
    SERVER_ERROR,
    /**
     *
     */
    UNKNOWN;


    private static final Set<ClientSyncState> APP_WITH_PERMISSION = EnumSet.of(OK, USAGE_LIMITS_EXCEEDED);
    /**
     * For these states an application is considered "with permissions" - it has permission to access the API but may have limits exceeded
     */
    public static boolean isStatePermitted(ClientSyncState syncState) {
        return APP_WITH_PERMISSION.contains(syncState);
    }


    private static final Set<ClientSyncState> ELIGABLE_OFFLINE_CONF = EnumSet.of(OK, USAGE_LIMITS_EXCEEDED);

    /**
     * In these cases the read config can be persisted as offline configuration replica
     */
    public static boolean isEligableOfflineConf(ClientSyncState syncState) {
        return ELIGABLE_OFFLINE_CONF.contains(syncState);
    }
}
