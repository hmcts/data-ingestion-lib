package uk.gov.hmcts.reform.data.ingestion.camel.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;
import uk.gov.hmcts.reform.data.ingestion.camel.service.dto.Email;

import java.io.IOException;

/**
 * This EmailServiceImpl send emails to intended recipients for failure cases
 * with detailed reason of failure.
 *
 * @since 2020-10-27
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
@Lazy
public class EmailServiceImpl implements IEmailService {

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    @Autowired(required = false)
    private SendGrid sendGrid;

    @Override
    public int sendEmail(final Email emailDto) {
        emailDto.validate();
        try {
            var personalization = new Personalization();
            emailDto.getTo().forEach(email ->
                personalization.addTo(new com.sendgrid.helpers.mail.objects.Email(email))
            );
            Content content = new Content("text/plain", emailDto.getMessageBody());
            Mail mail = new Mail();
            mail.setFrom(new com.sendgrid.helpers.mail.objects.Email(emailDto.getFrom()));
            mail.setSubject(emailDto.getSubject());
            mail.addContent(content);
            mail.addPersonalization(personalization);
            var request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            final Response response = sendGrid.api(request);
            log.info("{} response with status {} and body {}", logComponentName,
                    response.getStatusCode(), request.getBody());
            return response.getStatusCode();
        } catch (IOException ex) {
            log.error("{}:: Exception  while  sending mail  {}", logComponentName, ex.getMessage());
            throw new EmailFailureException(ex);
        }
    }
}
