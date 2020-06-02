package uk.gov.hmcts.reform.data.ingestion.camel.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailData {
    String recipient;
    String subject;
    String message;
}