package ch.sbb.integration.api.threescale.model;

import java.io.Serializable;

/**
 * Created by u217269 on 08.03.2018.
 */
public class Proxy implements Serializable {

    private final String targetUrl;

    public Proxy(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

}
