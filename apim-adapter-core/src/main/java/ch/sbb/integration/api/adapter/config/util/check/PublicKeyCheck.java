package ch.sbb.integration.api.adapter.config.util.check;

import ch.sbb.integration.api.adapter.config.ReasonCode;
import ch.sbb.integration.api.adapter.model.status.CheckResult;
import ch.sbb.integration.api.adapter.model.status.Status;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuer;
import ch.sbb.integration.api.adapter.model.tokenissuer.TokenIssuerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PublicKeyCheck {
    private static final Logger LOG = LoggerFactory.getLogger(PublicKeyCheck.class);
    private static final String NAME = "LoadOfPublicKeys from Token Issuers";

    public CheckResult checkPublicKey() {
        final TokenIssuerStore tokenIssuerStore = TokenIssuerStore.getInstance();
        if (tokenIssuerStore == null) {
            return new CheckResult(NAME, Status.DOWN, "Token Issuer Store is not yet initialized");
        }

        int issuersUp = 0;
        int issuersDown = 0;
        final List<String> errorMessages = new ArrayList<>();
        for (TokenIssuer issuer : tokenIssuerStore.getIssuers()) {
            try {
                issuer.loadJwks(true);
                issuersUp++;
            } catch (Exception e) {
                issuersDown++;
                final String msg = ReasonCode.APIM_2025.format(issuer.getJwksUri());
                errorMessages.add(msg);
                LOG.warn(msg, e);
            }
        }

        if (issuersUp > 0 && issuersDown == 0) {
            return new CheckResult(NAME, Status.UP, "Public keys were loaded for " + issuersUp + " issuers");
        } else if (issuersUp > 0 && issuersDown > 0) {
            return new CheckResult(NAME, Status.UP, "Public keys were only loaded for " + issuersUp + " out of " + tokenIssuerStore.getIssuers().size() + " issuers. Errors: \n" + String.join("\n", errorMessages));
        } else {
            return new CheckResult(NAME, Status.DOWN, "Public keys for no issuer present. Errors: \n" + String.join("\n", errorMessages));
        }
    }
}
