package ch.sbb.integration.api.adapter.model;

import java.io.Serializable;

/**
 * Created by u217269 on 08.03.2018.
 */
public class Proxy implements Serializable {

    private static final long serialVersionUID = 5918024053378013101L;

    private final String targetUrl;

    public Proxy(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    @Override
    public String toString() {
        return "Proxy{" +
                "targetUrl='" + targetUrl + '\'' +
                '}';
    }
}
