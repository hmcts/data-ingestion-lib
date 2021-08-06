package uk.gov.hmcts.reform.data.ingestion.camel.service;

import uk.gov.hmcts.reform.data.ingestion.camel.service.dto.Email;

public interface IEmailService {

    /**
     * Triggers failure mails with reason of failure.
     *
     * @param emailDto The dto object that holds all the details required to send an email.
     */
    void sendEmail(Email emailDto);

}
