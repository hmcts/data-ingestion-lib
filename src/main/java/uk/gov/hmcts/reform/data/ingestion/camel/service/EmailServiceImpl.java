package uk.gov.hmcts.reform.data.ingestion.camel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

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

    @Autowired
    JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String mailFrom;

    @Value("${spring.mail.to}")
    private String mailTo;

    @Value("${spring.mail.subject}")
    private String mailsubject;

    @Value("${spring.mail.enabled}")
    private boolean mailEnabled;

    @Value("${spring.esb.mail.enabled}")
    private boolean esbMailEnabled;

    @Value("${spring.esb.mail.subject}")
    private String esbMailSubject;

    @Value("${spring.esb.mail.to}")
    private String esbMailTo;

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    @Value("${ENV_NAME:''}")
    private String environmentName;

    /**
     * Triggers failure mails with reason of failure if mailing is enabled.
     *
     * @param messageBody String
     * @param filename String
     */
    public void sendEmail(String messageBody, String filename) {

        if (mailEnabled || esbMailEnabled) {
            try {
                //check mail flag and send mail
                final MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper mimeMsgHelperObj = new MimeMessageHelper(message, true);
                if (mailEnabled) {
                    mimeMsgHelperObj.setTo(mailTo.split(","));
                    filename = isNull(filename) ? EMPTY : filename;
                    mimeMsgHelperObj.setSubject(environmentName.concat("::" + mailsubject.concat(filename)));
                } else if (esbMailEnabled) {
                    mimeMsgHelperObj.setTo(esbMailTo.split(","));
                    mimeMsgHelperObj.setSubject(esbMailSubject);
                }
                mimeMsgHelperObj.setText(messageBody);
                mimeMsgHelperObj.setFrom(mailFrom);
                mailSender.send(mimeMsgHelperObj.getMimeMessage());
            } catch (MailException | MessagingException e) {
                log.error("{}:: Exception  while  sending mail  {}", logComponentName, getStackTrace(e));
                throw new EmailFailureException(e);
            }
        } else {
            log.info("{}:: Exception in data ingestion, but emails alerts has been disabled", logComponentName);
        }
    }

}
