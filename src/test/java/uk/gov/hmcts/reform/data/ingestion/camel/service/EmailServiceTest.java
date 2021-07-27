package uk.gov.hmcts.reform.data.ingestion.camel.service;


import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @InjectMocks
    EmailServiceImpl emailServiceImpl;

    @Mock
    JavaMailSender mailSender;
    String mailFrom;
    List<String> mailTo;
    String mailsubject;
    String messageBody;
    String filename;
    Boolean mailEnabled = true;

    @Mock
    SendGrid sendGrid;

    Response response = new Response();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        response.setBody("empty");
        response.setStatusCode(200);
        mockData();
    }

    private void mockData() {
        mailFrom = "no-reply@reform.hmcts.net";
        mailTo = new ArrayList<>();
        mailTo.add("example1@hmcts.net");
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
    @SneakyThrows
    public void testSendEmail() {
        when(sendGrid.api(any(Request.class))).thenReturn(response);
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