package uk.gov.hmcts.reform.data.ingestion.camel.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;

import javax.mail.internet.MimeMessage;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @InjectMocks
    EmailServiceImpl emailServiceImpl;

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


    @BeforeEach
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
        setField(emailServiceImpl, "mailFrom", mailFrom);
        setField(emailServiceImpl, "mailTo", mailTo);
        setField(emailServiceImpl, "mailsubject", mailsubject);
        setField(emailServiceImpl, "mailEnabled", Boolean.TRUE);
        setField(emailServiceImpl, "environmentName", "");
    }

    @Test
    public void testSendEmail() {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        emailServiceImpl.sendEmail(messageBody, filename);
        assertEquals("Test", messageBody);
        assertEquals("File1.csv", filename);
    }

    @Test
    public void testSendEmailException() {
        EmailFailureException emailFailureException = new EmailFailureException(new Throwable());
        doThrow(emailFailureException).when(mailSender).send(any(MimeMessage.class));
        assertThrows(EmailFailureException.class, () -> emailServiceImpl
            .sendEmail("Test", "File1.csv"));
    }

    @Test
    public void testMailException() {
        MailException emailFailureException = mock(MailException.class);
        doThrow(emailFailureException).when(mailSender).send(any(MimeMessage.class));
        assertThrows(EmailFailureException.class, () -> emailServiceImpl
            .sendEmail("Test", "File1.csv"));
    }
}