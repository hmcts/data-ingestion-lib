package uk.gov.hmcts.reform.data.ingestion.camel.service;


import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
    SendGrid sendGrid;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        mockData();
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
        emailServiceImpl.sendEmail(messageBody, filename);
        assertEquals("Test", messageBody);
        assertEquals("File1.csv", filename);
    }

    @Test
    @SneakyThrows
    public void testMailException() {
        doThrow(IOException.class).when(sendGrid).api(any(Request.class));
        assertThrows(EmailFailureException.class, () -> emailServiceImpl
            .sendEmail("Test", "File1.csv"));
    }
}