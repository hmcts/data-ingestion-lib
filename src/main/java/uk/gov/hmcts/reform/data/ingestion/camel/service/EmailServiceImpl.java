package uk.gov.hmcts.reform.data.ingestion.camel.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;

import java.io.IOException;

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
public class EmailServiceImpl implements IEmailService {

    @Value("${sendgrid.mail.from}")
    private String mailFrom;

    @Value("${spring.mail.to}")
    private String mailTo;

    @Value("${spring.mail.subject}")
    private String mailsubject;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.esb.mail.enabled:false}")
    private boolean esbMailEnabled;

    @Value("${spring.esb.mail.subject}")
    private String esbMailSubject;

    @Value("${spring.esb.mail.to}")
    private String esbMailTo;

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    @Value("${ENV_NAME:''}")
    private String environmentName;

    @Autowired
    private SendGrid sendGrid;

    /**
     * Triggers failure mails with reason of failure if mailing is enabled.
     *
     * @param messageBody String
     * @param filename    String
     */
    //TODO: Need to refactor this code
    @Override
    public void sendEmail(String messageBody, String filename) {

        // mailEnabled and esbMailEnabled cannot be TRUE at the same time.
        if (mailEnabled || esbMailEnabled) {
            if (mailEnabled) {
                filename = isNull(filename) ? EMPTY : filename;
                mailsubject = environmentName.concat("::" + mailsubject.concat(filename));
                sendMail(mailTo, mailsubject, messageBody);
            } else if (esbMailEnabled) {
                sendMail(esbMailTo, esbMailSubject, messageBody);
            }
        } else {
            log.info("{}:: Exception in data ingestion, but emails alerts has been disabled", logComponentName);
        }
    }


    private void sendMail(String emailTo, String emailSubject, String messageBody) {
        try {
            Request request = new Request();
            Email from = new Email(mailFrom);
            Email to = new Email(emailTo);
            Content content = new Content("text/plain", messageBody);
            Mail mail = new Mail(from, emailSubject, to, content);
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sendGrid.api(request);
        } catch (IOException ex) {
            log.error("{}:: Exception  while  sending mail  {}", logComponentName, ex.getMessage());
            throw new EmailFailureException(ex);
        }
    }

    @Override
    public void setEsbMailEnabled(boolean esbMailEnabled) {
        this.esbMailEnabled = esbMailEnabled;
    }
}
