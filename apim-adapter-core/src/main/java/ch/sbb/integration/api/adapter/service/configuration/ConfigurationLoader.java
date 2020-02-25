package ch.sbb.integration.api.adapter.service.configuration;

import ch.sbb.integration.api.adapter.model.ThreeScalePlanResult;
import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.model.usage.ClientSyncState;
import ch.sbb.integration.api.adapter.service.converter.ClientConverter;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.service.repository.OfflineConfigurationCacheRepo;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleAdminCommunicationComponent;
import ch.sbb.integration.api.adapter.service.restclient.ThreeScaleBackendCommunicationComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;

import static ch.sbb.integration.api.adapter.config.ReasonCode.*;
import static ch.sbb.integration.api.adapter.model.ConfigType.MAPPING_RULES;
import static ch.sbb.integration.api.adapter.model.ConfigType.METRIC;
import static ch.sbb.integration.api.adapter.model.ConfigType.PLAN;
import static ch.sbb.integration.api.adapter.model.ConfigType.PROXY;
import static ch.sbb.integration.api.adapter.model.usage.ClientSyncState.APPLICATION_NOT_FOUND;
import static ch.sbb.integration.api.adapter.model.usage.ClientSyncState.SERVER_ERROR;
import static ch.sbb.integration.api.adapter.model.usage.ClientSyncState.UNKNOWN;

public class ConfigurationLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationLoader.class);

    private final OfflineConfigurationCacheRepo repo;
    private final EmergencyModeState emergencyModeState;
    private final ThreeScaleAdminCommunicationComponent threeScaleAdminCommunicationComponent;
    private final ThreeScaleBackendCommunicationComponent threeScaleBackendCommunicationComponent;
    private final ClientConverter clientConverter;

    public ConfigurationLoader(OfflineConfigurationCacheRepo repo,
                               EmergencyModeState emergencyModeState,
                               ThreeScaleAdminCommunicationComponent threeScaleAdminCommunicationComponent,
                               ThreeScaleBackendCommunicationComponent threeScaleBackendCommunicationComponent) {
        this.repo = repo;
        this.emergencyModeState = emergencyModeState;
        this.threeScaleAdminCommunicationComponent = threeScaleAdminCommunicationComponent;
        this.threeScaleBackendCommunicationComponent = threeScaleBackendCommunicationComponent;
        this.clientConverter = new ClientConverter();
    }

    public Client loadPlanConfig(String serviceId, String clientId, boolean loadFromOfflineCache) throws IOException, SAXException, ParserConfigurationException {
        try {
            ThreeScalePlanResult threeScalePlanResult = threeScaleBackendCommunicationComponent.loadThreeScalePlan(clientId);
            final Client client = clientConverter.convertToClient(clientId, threeScalePlanResult.getStatusCode(), threeScalePlanResult.getXmlContent());
            if (ClientSyncState.isEligableOfflineConf(client.getSyncState())) {
                emergencyModeState.setConfigurationSuccessfulLoaded(PLAN, true);
                repo.persistPlanConfig(serviceId, clientId, threeScalePlanResult.getXmlContent());
            }

            if (SERVER_ERROR == client.getSyncState()) {
                if (loadFromOfflineCache) {
                    return doLoadPlanFromOfflineCache(serviceId, clientId);
                }
            } else if (APPLICATION_NOT_FOUND == client.getSyncState()) {
                repo.deletePlanConfig(serviceId, clientId);
            } else if (UNKNOWN == client.getSyncState()) {
                LOG.warn(APIM_2011.pattern(), clientId, serviceId, threeScalePlanResult);
                repo.deletePlanConfig(serviceId, clientId);
            }

            return client;

        } catch (Exception ex) {
            if (loadFromOfflineCache) {
                return doLoadPlanFromOfflineCache(serviceId, clientId);
            } else {
                LOG.info(APIM_1013.pattern(), ex);
                return clientConverter.convertToClient(clientId, HttpResponseCodes.SC_INTERNAL_SERVER_ERROR, null);
            }
        }
    }

    private Client doLoadPlanFromOfflineCache(String serviceId, String clientId) {
        emergencyModeState.setConfigurationSuccessfulLoaded(PLAN, false);
        LOG.warn(APIM_2012.pattern(), serviceId, clientId);
        try {
            String xmlContent = repo.findPlanConfig(serviceId, clientId);
            return clientConverter.convertToClient(clientId, HttpResponseCodes.SC_OK, xmlContent);
        } catch (Exception ex) {
            LOG.info(APIM_1014.pattern(), serviceId, clientId, ex);
            return new Client(clientId, new HashMap<>(), SERVER_ERROR);
        }
    }

    public JsonNode loadMappingRulesConfig(String serviceId, boolean loadFromOfflineCache) {
        String mappingRulesJson = doLoadMappingRulesConfig(serviceId, loadFromOfflineCache);

        try {
            return new ObjectMapper().readTree(mappingRulesJson).get("mapping_rules");
        } catch (Exception e) {
            throw new ThreeScaleAdapterException(APIM_3021.pattern(), e);
        }
    }

    private String doLoadMappingRulesConfig(String serviceId, boolean loadFromOfflineCache) {
        try {
            String mappingRules = threeScaleAdminCommunicationComponent.loadMappingRulesConfig(serviceId);
            emergencyModeState.setConfigurationSuccessfulLoaded(MAPPING_RULES, true);
            repo.persistMappingRulesConfig(serviceId, mappingRules);
            return mappingRules;
        } catch (Exception ex) {
            if (loadFromOfflineCache) {
                emergencyModeState.setConfigurationSuccessfulLoaded(MAPPING_RULES, false);
                LOG.warn(APIM_2013.pattern(), serviceId, ex.getMessage());
                return repo.findMappingRulesConfig(serviceId);
            } else {
                throw ex;
            }
        }
    }

    public JsonNode loadMetricConfig(String serviceId, boolean loadFromOfflineCache) {
        String metric = doLoadMetricConfig(serviceId, loadFromOfflineCache);

        try {
            return new ObjectMapper().readTree(metric);
        } catch (Exception e) {
            throw new ThreeScaleAdapterException(APIM_3008.pattern(), e);
        }
    }

    private String doLoadMetricConfig(String serviceId, boolean loadFromOfflineCache) {
        try {
            String metricConfig = threeScaleAdminCommunicationComponent.loadMetricConfig(serviceId);
            emergencyModeState.setConfigurationSuccessfulLoaded(METRIC, true);
            repo.persistMetricConfig(serviceId, metricConfig);
            return metricConfig;
        } catch (Exception ex) {
            if (loadFromOfflineCache) {
                emergencyModeState.setConfigurationSuccessfulLoaded(METRIC, false);
                LOG.warn(APIM_2014.pattern(), serviceId, ex.getMessage());
                return repo.findMetricConfig(serviceId);
            } else {
                throw ex;
            }
        }
    }

    public JsonNode loadProxyConfig(String serviceId, boolean loadFromOfflineCache) {
        String proxySettings = doLoadProxyConfig(serviceId, loadFromOfflineCache);

        try {
            return new ObjectMapper().readTree(proxySettings);
        } catch (Exception e) {
            throw new ThreeScaleAdapterException(APIM_3018.pattern(), e);
        }
    }

    private String doLoadProxyConfig(String serviceId, boolean loadFromOfflineCache) {
        try {
            String proxyConfig = threeScaleAdminCommunicationComponent.loadProxyConfig(serviceId);
            emergencyModeState.setConfigurationSuccessfulLoaded(PROXY, true);
            repo.persistProxyConfig(serviceId, proxyConfig);
            return proxyConfig;
        } catch (Exception ex) {
            if (loadFromOfflineCache) {
                emergencyModeState.setConfigurationSuccessfulLoaded(PROXY, false);
                LOG.warn(APIM_2015.pattern(), serviceId, ex.getMessage());
                return repo.findProxyConfig(serviceId);
            } else {
                throw ex;
            }
        }
    }

}
