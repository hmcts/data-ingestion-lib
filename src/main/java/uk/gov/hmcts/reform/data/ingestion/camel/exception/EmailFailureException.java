package uk.gov.hmcts.reform.data.ingestion.camel.exception;

public class EmailFailureException extends RuntimeException {
    public EmailFailureException(Throwable cause) {
        super(cause);
    }
}