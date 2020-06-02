package uk.gov.hmcts.reform.data.ingestion.camel.exception;

public class RouteFailedException extends RuntimeException {

    public RouteFailedException(String message) {
        super(message);
    }
}
