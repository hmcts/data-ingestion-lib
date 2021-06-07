package uk.gov.hmcts.reform.data.ingestion.camel.service;

public interface IEmailService {

    void setEsbMailEnabled(boolean esbMailEnabled);

    void sendEmail(String messageBody, String filename);
}
