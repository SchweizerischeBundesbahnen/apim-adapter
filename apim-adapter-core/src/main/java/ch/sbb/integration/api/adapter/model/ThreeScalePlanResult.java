package ch.sbb.integration.api.adapter.model;

public class ThreeScalePlanResult {

    private final String xmlContent;
    private final int statusCode;

    public ThreeScalePlanResult(String xmlContent, int statusCode) {
        this.xmlContent = xmlContent;
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return "ThreeScalePlanResult{" +
                "xmlContent='" + xmlContent + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
