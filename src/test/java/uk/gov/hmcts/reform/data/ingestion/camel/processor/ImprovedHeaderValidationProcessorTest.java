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

public class ImprovedHeaderValidationProcessorTest {

    Exchange exchangeMock = mock(Exchange.class);
    Message messageMock = mock(Message.class);
    ApplicationContext applicationContextMock = mock(ApplicationContext.class);
    CamelContext camelContext = mock(CamelContext.class);
    HeaderValidationProcessor headerValidationProcessor = spy(new HeaderValidationProcessor());

    static class BinderObject {
        private String field1;

        private String field2;
    }

    @BeforeEach
    public void setUp() throws Exception {
        when(exchangeMock.getIn()).thenReturn(messageMock);
        setField(headerValidationProcessor.getClass()
                .getDeclaredField("applicationContext"), headerValidationProcessor, applicationContextMock);
        setField(headerValidationProcessor.getClass()
                .getDeclaredField("camelContext"), headerValidationProcessor, camelContext);
    }

    @SneakyThrows
    @Test
    public void testProcess() {
        setRouteProperties("true");
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("field1,field2");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        headerValidationProcessor.process(exchangeMock);
        verify(headerValidationProcessor).process(exchangeMock);
    }

    @SneakyThrows
    @Test
    public void testProcess_CaseSensitivityCheck() {
        setRouteProperties("true");
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("Field1,FIELD2");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        headerValidationProcessor.process(exchangeMock);
        verify(headerValidationProcessor).process(exchangeMock);
    }


    @SneakyThrows
    @Test
    public void testProcessException_WhenHeaderValidationToggledOn() {
        setRouteProperties("true");
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("field1,field2,field3");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(camelContext.getGlobalOptions()).thenReturn(new HashMap<>());
        assertThrows(RouteFailedException.class, () -> headerValidationProcessor.process(exchangeMock));
        verify(headerValidationProcessor).process(exchangeMock);
    }

    @SneakyThrows
    @Test
    public void testProcessException_WhenHeaderValidationToggledOff() {
        setRouteProperties("false");
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("field1,field2,field3");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(camelContext.getGlobalOptions()).thenReturn(new HashMap<>());
        assertThrows(RouteFailedException.class, () -> headerValidationProcessor.process(exchangeMock));
        verify(headerValidationProcessor).process(exchangeMock);
    }

    @SneakyThrows
    @Test
    public void testProcessException_JumbledOrder() {
        setRouteProperties("true");
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("field2,field1");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(camelContext.getGlobalOptions()).thenReturn(new HashMap<>());
        assertThrows(RouteFailedException.class, () -> headerValidationProcessor.process(exchangeMock));
        verify(headerValidationProcessor).process(exchangeMock);
    }

    @SneakyThrows
    @Test
    public void testProcess_NoException_HeadersJumbledOrder_WhenHeaderValidationToggledOff() {
        setRouteProperties("false");
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("field2,field1");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(camelContext.getGlobalOptions()).thenReturn(new HashMap<>());
        headerValidationProcessor.process(exchangeMock);
        verify(headerValidationProcessor).process(exchangeMock);
    }

    private void setRouteProperties(String isHeaderValidationEnabled) {
        RouteProperties routeProperties = new RouteProperties();
        routeProperties.setBinder("binderObject");
        routeProperties.setCsvHeadersExpected("field1,field2");
        routeProperties.setIsHeaderValidationEnabled(isHeaderValidationEnabled);
        when(exchangeMock.getIn().getHeader(MappingConstants.ROUTE_DETAILS)).thenReturn(routeProperties);

        ImprovedHeaderValidationProcessorTest.BinderObject binderObject =
                new ImprovedHeaderValidationProcessorTest.BinderObject();
        when(applicationContextMock.getBean(routeProperties.getBinder())).thenReturn(binderObject);
    }

}
