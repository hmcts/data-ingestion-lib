package uk.gov.hmcts.reform.data.ingestion.camel.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;

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

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    @Value("${environment:''}")
    private String environmentName;

    public void sendEmail(String messageBody, String filename) {

        if (mailEnabled) {
            try {
                //check mail flag and send mail
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper mimeMsgHelperObj = new MimeMessageHelper(message, true);
                String[] split = mailTo.split(",");
                mimeMsgHelperObj.setTo(split);
                filename = isNull(filename) ? EMPTY : filename;
                mimeMsgHelperObj.setSubject(environmentName.concat("::" + mailsubject.concat(filename)));
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
