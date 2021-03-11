package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants;

import java.util.HashMap;

import static org.apache.camel.spring.util.ReflectionUtils.setField;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeaderValidationProcessorTest {
    Exchange exchangeMock = mock(Exchange.class);
    Message messageMock = mock(Message.class);
    RouteProperties routePropertiesMock = mock(RouteProperties.class);
    ApplicationContext applicationContextMock = mock(ApplicationContext.class);
    CamelContext camelContext = mock(CamelContext.class);
    HeaderValidationProcessor headerValidationProcessor = spy(new HeaderValidationProcessor());

    static class BinderObject {
    }

    @BeforeEach
    public void setUp() throws Exception {
        when(exchangeMock.getIn()).thenReturn(messageMock);
        when(exchangeMock.getIn().getHeader(MappingConstants.ROUTE_DETAILS)).thenReturn(routePropertiesMock);
        setField(headerValidationProcessor.getClass()
            .getDeclaredField("applicationContext"), headerValidationProcessor, applicationContextMock);
        setField(headerValidationProcessor.getClass()
            .getDeclaredField("camelContext"), headerValidationProcessor, camelContext);
    }

    @SneakyThrows
    @Test
    public void testProcess() {
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("field");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(applicationContextMock.getBean(routePropertiesMock.getBinder())).thenReturn(BinderObject.class);
        headerValidationProcessor.process(exchangeMock);
        verify(headerValidationProcessor).process(exchangeMock);
    }

    @SneakyThrows
    @Test
    public void testProcessException() {
        BinderObject binderObject = new BinderObject();
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("filed1,field2");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(camelContext.getGlobalOptions()).thenReturn(new HashMap<>());
        when(applicationContextMock.getBean(routePropertiesMock.getBinder())).thenReturn(binderObject);
        assertThrows(RouteFailedException.class, () -> headerValidationProcessor.process(exchangeMock));
        verify(headerValidationProcessor).process(exchangeMock);
    }
}