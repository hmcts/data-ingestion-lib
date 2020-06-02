package uk.gov.hmcts.reform.ingestion.camel.exception;

public class RouteFailedException extends RuntimeException {

    public RouteFailedException(String message) {
        super(message);
    }
}
