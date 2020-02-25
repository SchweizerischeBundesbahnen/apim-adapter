package ch.sbb.integration.api.adapter.service.converter;

import ch.sbb.integration.api.adapter.model.usage.Client;
import ch.sbb.integration.api.adapter.model.usage.ClientSyncState;
import ch.sbb.integration.api.adapter.model.usage.MetricUsage;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_1017;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2002;

public class ClientConverter {
    private static final String ETERNITY_PERIOD_END = "9999-12-31 23:59:59 +0000";
    private static final String ETERNITY_PERIOD_START = "1970-01-01 00:00:00 +0000";

    private static final Logger LOG = LoggerFactory.getLogger(ClientConverter.class);

    public Client convertToClient(String clientId, int statusCode, String xml) throws ParserConfigurationException, SAXException, IOException {
        LOG.debug("oauthAuthorize returned the following xml for client '{}': statuscode='{}' xml: '{}'", clientId, statusCode, xml);

        Map<String, MetricUsage> limitedMetricUsages  = new HashMap<>();

        final ClientSyncState state;
        if (isServerError(statusCode)) {
            state = ClientSyncState.SERVER_ERROR;
        } else if (isApplicationNotFound(clientId, statusCode, xml)) {
            state = ClientSyncState.APPLICATION_NOT_FOUND;
        } else if (xml == null || xml.isEmpty()) {
            LOG.warn(APIM_2002.pattern(), clientId, statusCode);
            state = ClientSyncState.UNKNOWN;
        } else {
            final Element xmlRoot = createXMLRoot(xml);
            limitedMetricUsages = parseLimitedMetricUsagesFromXml(xmlRoot, clientId);

            if (statusCode == HttpResponseCodes.SC_OK) {
                state = ClientSyncState.OK;
            }else if (isConflictAndLimitExceeded(statusCode, xmlRoot)) {
                state = ClientSyncState.USAGE_LIMITS_EXCEEDED;
            } else {
                state = ClientSyncState.UNKNOWN;
            }
        }

        return new Client(clientId, limitedMetricUsages, state);
    }

    private boolean isServerError(int statusCode) {
        return Response.Status.Family.familyOf(statusCode) == Response.Status.Family.SERVER_ERROR;
    }

    private boolean isApplicationNotFound(String clientId, int statusCode, String xml) {
        if (HttpResponseCodes.SC_NOT_FOUND == statusCode) {
            LOG.info(APIM_1017.pattern(), clientId, statusCode, xml);
            return xml != null && xml.contains("application_not_found");
        }
        return false;
    }

    private static boolean isConflictAndLimitExceeded(int statusCode, Element xmlRoot) {
        if (statusCode == HttpResponseCodes.SC_CONFLICT) {
            Node reasonNode = xmlRoot.getElementsByTagName("reason").item(0);
            if(reasonNode != null) {
                String reason = reasonNode.getTextContent();
                return "usage limits are exceeded".equalsIgnoreCase(reason);
            }
        }
        return false;
    }

    private static Element createXMLRoot(String xml) throws ParserConfigurationException, SAXException, IOException {
        InputSource inputSource = new InputSource(new StringReader(xml));
        inputSource.setEncoding("UTF-8");

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(inputSource);

        Element root = xmlDocument.getDocumentElement();
        root.normalize();

        return root;
    }

    private static Map<String, MetricUsage> parseLimitedMetricUsagesFromXml(Element root, String clientId) {
        Map<String, MetricUsage> metricUsageMap = new HashMap<>();

        boolean isLimitedPlan = root.getElementsByTagName("usage_reports").getLength() > 0;
        if (isLimitedPlan) {
            NodeList usageReports = ((Element) root.getElementsByTagName("usage_reports").item(0)).getElementsByTagName("usage_report");
            for (int i = 0; i < usageReports.getLength(); i++) {
                Element usageReport = (Element) usageReports.item(i);

                String metric = usageReport.getAttributes().getNamedItem("metric").getTextContent();
                String period = usageReport.getAttributes().getNamedItem("period").getTextContent();
                String periodEnd = ETERNITY_PERIOD_END;
                String periodStart = ETERNITY_PERIOD_START;
                if (!"eternity".equalsIgnoreCase(period)) {
                    periodStart = usageReport.getElementsByTagName("period_start").item(0).getTextContent();
                    periodEnd = usageReport.getElementsByTagName("period_end").item(0).getTextContent();
                }
                Long maxValue = Long.valueOf(usageReport.getElementsByTagName("max_value").item(0).getTextContent());
                Long currentValue = Long.valueOf(usageReport.getElementsByTagName("current_value").item(0).getTextContent());

                metricUsageMap.put(metric, MetricUsage.limitedMetric(clientId, metric, maxValue, currentValue, periodStart, periodEnd));
            }
        }
        return metricUsageMap;
    }
}
