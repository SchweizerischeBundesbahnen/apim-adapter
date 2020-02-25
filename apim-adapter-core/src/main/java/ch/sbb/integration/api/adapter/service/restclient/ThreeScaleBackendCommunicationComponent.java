package ch.sbb.integration.api.adapter.service.restclient;

import ch.sbb.integration.api.adapter.config.ApimAdapterConfig;
import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.config.RestConfig;
import ch.sbb.integration.api.adapter.model.ThreeScalePlanResult;
import ch.sbb.integration.api.adapter.model.reporting.ResponseSummary;
import ch.sbb.integration.api.adapter.service.exception.ThreeScaleAdapterException;
import ch.sbb.integration.api.adapter.service.utils.StopWatch;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2028;
import static ch.sbb.integration.api.adapter.config.ReasonCode.APIM_2029;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class ThreeScaleBackendCommunicationComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ThreeScaleBackendCommunicationComponent.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
    private static final int MAX_TRANSACTIONS_PER_FORM = 1000;

    private final ResteasyClient rest;
    private final String serviceToken;
    private final String serviceId;
    private final String backendUrl;
    private final boolean reportResponseCode;

    public ThreeScaleBackendCommunicationComponent(ApimAdapterConfig config, RestConfig restConfig) {
        this.backendUrl = config.getBackendUrl();
        this.serviceToken = config.getBackendToken();
        this.serviceId = config.getAdapterServiceId();
        this.reportResponseCode = config.isReportResponseCode();

        this.rest = restConfig.newRestEasyClient();
    }

    public boolean report(Map<ResponseSummary, Long> hits) {
        List<Map<String, String>> transactions = createTransactions(hits);
        List<Form> forms = splitAndTransform(transactions);
        Long numberOfHits = hits.values().stream().reduce(0L, Long::sum);

        LOG.info(ReasonCode.APIM_1024.pattern(), numberOfHits);
        LOG.debug("Reporting to 3Scale - number of hits={}, size of hits={}, number of transactions={}, number of forms={}", numberOfHits, hits.size(), transactions.size(), forms.size());

        for (Form f : forms) {
            boolean successful = submitForm(f);
            if (!successful) {
                //If one report failed, we mark all reporting as failed.
                return false;
            }
        }

        return true;
    }

    private boolean submitForm(Form form) {
        final StopWatch sw = new StopWatch().start();
        final String url = backendUrl + "/transactions.xml"; // Does only exist in XML unfortunately
        try (Response response = rest.target(url).request().post(Entity.form(form))) {
            LOG.debug("reported transactions to 3scale, result={}  duration={} ms", response.getStatusInfo(), sw.stop().getMillis());
            if (SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                return true;
            } else {
                LOG.info(ReasonCode.APIM_1025.pattern(), response.getStatusInfo().getStatusCode(), response);
                return false;
            }
        } catch (Exception ex) {
            LOG.warn(APIM_2028.pattern(), sw.stop().getMillis(), ex);
            return false;
        }
    }

    private List<Form> splitAndTransform(List<Map<String, String>> transactions) {
        final AtomicInteger counter = new AtomicInteger();
        Collection<List<Map<String, String>>> transactionChunk = transactions.stream()
                .collect(groupingBy(it -> counter.getAndIncrement() / MAX_TRANSACTIONS_PER_FORM)) //We split the list into chunks
                .values();

        return transactionChunk.stream()
                .map(this::to3ScaleTransactionForm)
                .collect(toList());
    }

    private Form to3ScaleTransactionForm(List<Map<String, String>> transactions) {
        Form form = new Form()
                .param("service_token", serviceToken)
                .param("service_id", serviceId);

        for (Map<String, String> transaction : transactions) {
            for (Map.Entry<String, String> transactionEntry : transaction.entrySet()) {
                form.param(transactionEntry.getKey(), transactionEntry.getValue());
            }
        }
        return form;
    }

    private List<Map<String, String>> createTransactions(Map<ResponseSummary, Long> hits) {
        if (reportResponseCode) {
            return createUsageAndResponseCodeTransaction(hits);
        } else {
            return createUsageTransaction(hits);
        }
    }

    private List<Map<String, String>> createUsageTransaction(Map<ResponseSummary, Long> hits) {
        List<Map<String, String>> transactions = new ArrayList<>();

        for (Map.Entry<ResponseSummary, Long> entry : hits.entrySet()) {
            //We make one transaction per responseSummary
            ResponseSummary responseSummary = entry.getKey();
            Long count = entry.getValue();
            Map<String, String> transaction = new HashMap<>();
            final int transactionNumber = transactions.size();
            transaction.put("transactions[" + transactionNumber + "][app_id]", responseSummary.getClientId());
            transaction.put("transactions[" + transactionNumber + "][timestamp]", responseSummary.getTimestamp().format(FORMATTER));
            transaction.put("transactions[" + transactionNumber + "][usage][" + responseSummary.getMetricSysName() + "]", String.valueOf(count));
            //We do not add [log][code]
            transactions.add(transaction);
        }

        return transactions;
    }

    private List<Map<String, String>> createUsageAndResponseCodeTransaction(Map<ResponseSummary, Long> hits) {
        List<Map<String, String>> transactions = new ArrayList<>();

        for (Map.Entry<ResponseSummary, Long> entry : hits.entrySet()) {
            ResponseSummary responseSummary = entry.getKey();
            Long count = entry.getValue();

            //We make one transaction per hit, because 3Scale cannot aggregate [log][code]
            for (int i = 0; i < count; i++) {
                Map<String, String> transaction = new HashMap<>();
                final int transactionNumber = transactions.size();
                transaction.put("transactions[" + transactionNumber + "][app_id]", responseSummary.getClientId());
                transaction.put("transactions[" + transactionNumber + "][timestamp]", responseSummary.getTimestamp().format(FORMATTER));

                transaction.put("transactions[" + transactionNumber + "][usage][" + responseSummary.getMetricSysName() + "]", "1");
                transaction.put("transactions[" + transactionNumber + "][log][code]", String.valueOf(responseSummary.getHttpStatus()));
                transactions.add(transaction);
            }
        }

        return transactions;
    }

    public ThreeScalePlanResult loadThreeScalePlan(String clientId) {
        final StopWatch sw = new StopWatch().start();
        final String url = backendUrl + "/transactions/oauth_authorize.xml"; // Does only exist in XML unfortunately
        try (ClientResponse response = (ClientResponse) rest.target(url)
                .queryParam("service_token", serviceToken)
                .queryParam("service_id", serviceId)
                .queryParam("app_id", clientId)
                .request()
                .get()) {
            LOG.debug("loaded plan: client={} status={} duration={} ms", clientId, response.getStatus(), sw.stop().getMillis());
            final String xml = response.readEntity(String.class);
            return new ThreeScalePlanResult(xml, response.getStatus());
        } catch (Exception e) {
            LOG.warn(APIM_2029.pattern(), clientId, sw.stop().getMillis());
            throw new ThreeScaleAdapterException(String.format("Error when loading the initial client usage stats. ServiceId: %s, ClientId: %s, url: %s", serviceId, clientId, url), e);
        }
    }
}
