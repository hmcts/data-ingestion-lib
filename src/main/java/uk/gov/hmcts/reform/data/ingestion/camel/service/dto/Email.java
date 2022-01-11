package uk.gov.hmcts.reform.data.ingestion.camel.service.dto;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;

import java.util.List;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@Data
@Builder
public class Email {

    private String from;

    private String subject;

    private List<String> to;

    private String contentType;

    private String messageBody;

    public void validate() {
        if (isNull(from) || isEmpty(to) || isNull(subject)) {
            throw new EmailFailureException("Can't send email as some of the mandatory email parameters are missing - "
                    + " From: {" + from + "} To: {" + to + "} Subject: {" + subject + "}.");
        }
    }
}
