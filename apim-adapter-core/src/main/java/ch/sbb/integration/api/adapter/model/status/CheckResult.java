package ch.sbb.integration.api.adapter.model.status;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class CheckResult {

    private String name;
    private Status status;
    private String message;
    private List<CheckResult> checkResults = new ArrayList<>();

    public CheckResult(String name, Status status, String message) {
        this.name = name;
        this.status = status;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @JsonIgnore
    public boolean isUp() {
        return Status.UP.equals(status);
    }

    public List<CheckResult> getCheckResults() {
        return checkResults;
    }

    public void addCheck(CheckResult checkResult) {
        this.checkResults.add(checkResult);
        this.status = checkResults.stream().anyMatch(result -> result.getStatus().equals(Status.DOWN)) ? Status.DOWN : Status.UP;
    }

    @Override
    public String toString() {
        return "CheckResult{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", checkResults=" + checkResults +
                '}';
    }
}
