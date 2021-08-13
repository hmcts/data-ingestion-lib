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
import uk.gov.hmcts.reform.data.ingestion.camel.exception.EmailFailureException;
import uk.gov.hmcts.reform.data.ingestion.camel.service.dto.Email;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @InjectMocks
    EmailServiceImpl emailServiceImpl;

    Email emailDto;

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
        var mailTo = new ArrayList<String>();
        mailTo.add("example1@hmcts.net");
        emailDto = Email
                .builder()
                .from("no-reply@reform.hmcts.net")
                .to(mailTo)
                .subject("Test mail")
                .messageBody("Test")
                .build();
    }

    @Test
    @SneakyThrows
    void testSendEmail() {
        when(sendGrid.api(any(Request.class))).thenReturn(response);
        emailServiceImpl.sendEmail(emailDto);
        assertEquals("Test", emailDto.getMessageBody());
        assertEquals("Test mail", emailDto.getSubject());
    }
    
    @Test
    @SneakyThrows
    void testSendEmail_WhenNonMandatoryParametersNotPassed() {
        when(sendGrid.api(any(Request.class))).thenReturn(response);
        emailDto.setMessageBody(null);
        emailServiceImpl.sendEmail(emailDto);
        assertEquals("Test mail", emailDto.getSubject());
    }

    @Test
    @SneakyThrows
    void testMailException() {
        doThrow(IOException.class).when(sendGrid).api(any(Request.class));
        assertThrows(EmailFailureException.class, () -> emailServiceImpl
            .sendEmail(emailDto));
    }

    @Test
    @SneakyThrows
    void testMailException_NoMailToGiven() {
        emailDto.setTo(null);
        assertThrows(EmailFailureException.class, () -> emailServiceImpl
                .sendEmail(emailDto));
    }

    @Test
    @SneakyThrows
    void testMailException_NoMailFromGiven() {
        emailDto.setFrom(null);
        assertThrows(EmailFailureException.class, () -> emailServiceImpl
                .sendEmail(emailDto));
    }

    @Test
    @SneakyThrows
    void testMailException_NoMailSubjectGiven() {
        emailDto.setSubject(null);
        assertThrows(EmailFailureException.class, () -> emailServiceImpl
                .sendEmail(emailDto));
    }
}