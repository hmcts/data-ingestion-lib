package uk.gov.hmcts.reform.data.ingestion.camel.service;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;
import uk.gov.hmcts.reform.data.ingestion.camel.service.EmailService;


public class EmailServiceTest {

    @InjectMocks
    EmailService emailService;

    @Mock
    JavaMailSender mailSender;
    String mailFrom;
    String mailTo;
    String mailsubject;
    String messageBody;
    String filename;
    Boolean mailEnabled = true;
    @Mock
    private MimeMessage mimeMessage;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mockData();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    }

    private void mockData() {
        mailFrom = "no-reply@reform.hmcts.net";
        mailTo = "example1@hmcts.net,example2@hmcts.net";
        mailsubject = "Test mail";
        messageBody = "Test";
        filename = "File1.csv";
        mailEnabled = true;
        setField(emailService, "mailFrom", mailFrom);
        setField(emailService, "mailTo", mailTo);
        setField(emailService, "mailsubject", mailsubject);
        setField(emailService, "mailEnabled", Boolean.TRUE);
    }

    @Test
    public void testSendEmail() {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        emailService.sendEmail(messageBody, filename);
        assertEquals("Test", messageBody);
        assertEquals("File1.csv", filename);

    }

    @Test(expected = EmailFailureException.class)
    public void testSendEmailException() {
        EmailFailureException emailFailureException = new EmailFailureException(new Throwable());
        doThrow(emailFailureException).when(mailSender).send(any(MimeMessage.class));
        emailService.sendEmail("Test", "File1.csv");
    }

    @Test(expected = EmailFailureException.class)
    public void testMailException() {
        MailException emailFailureException = mock(MailException.class);
        doThrow(emailFailureException).when(mailSender).send(any(MimeMessage.class));
        emailService.sendEmail("Test", "File1.csv");
    }
}