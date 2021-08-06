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
import java.util.List;

import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.EMPTY;

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

    private static final String EMAIL_SUBJECT = "%s::%s %s";

    @Override
    public void sendEmail(Email emailDto) {
        emailDto.validate();
        String filename = emailDto.getFileName();
        filename = isNull(filename) ? EMPTY : filename;
        String subject = String.format(EMAIL_SUBJECT, emailDto.getEnvironment(), emailDto.getSubject(), filename);
        sendMail(emailDto.getTo(), subject, emailDto.getMessageBody(), emailDto.getFrom());
    }

    private void sendMail(List<String> emailTo, String emailSubject, String messageBody, String from) {
        try {
            var personalization = new Personalization();
            emailTo.forEach(email -> {
                personalization.addTo(new com.sendgrid.helpers.mail.objects.Email(email));
            });
            Content content = new Content("text/plain", messageBody);
            Mail mail = new Mail();
            mail.setFrom(new com.sendgrid.helpers.mail.objects.Email(from));
            mail.setSubject(emailSubject);
            mail.addContent(content);
            mail.addPersonalization(personalization);
            var request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            log.info("{} response with status {} and body {}", logComponentName,
                response.getStatusCode(), request.getBody());
        } catch (IOException ex) {
            log.error("{}:: Exception  while  sending mail  {}", logComponentName, ex.getMessage());
            throw new EmailFailureException(ex);
        }
    }
}
