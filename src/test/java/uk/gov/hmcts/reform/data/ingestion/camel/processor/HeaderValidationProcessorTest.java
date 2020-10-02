package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import lombok.SneakyThrows;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HeaderValidationProcessorTest {

    @InjectMocks
    HeaderValidationProcessor headerValidationProcessor;

    Exchange exchangeMock = mock(Exchange.class);
    Message messageMock = mock(Message.class);
    RouteProperties routePropertiesMock = mock(RouteProperties.class);
    ApplicationContext applicationContextMock = mock(ApplicationContext.class);

    class JudicialUserRoleType {
    }

    @Before
    public void setUp() {
        when(exchangeMock.getIn()).thenReturn(messageMock);
        when(exchangeMock.getIn().getHeader(MappingConstants.ROUTE_DETAILS)).thenReturn(routePropertiesMock);

        MockitoAnnotations.initMocks(this);
    }

    @SneakyThrows
    @Test(expected = RouteFailedException.class)
    public void testProcessThrowsRouteFailedExceptionWhenExchangeBodyIsNull() {
        when(routePropertiesMock.getFileName()).thenReturn("Locations");

        headerValidationProcessor.process(exchangeMock);
    }

    @SneakyThrows
    @Test
    public void testProcess() {
        when(exchangeMock.getIn().getBody(String.class)).thenReturn("Body");
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(applicationContextMock.getBean(routePropertiesMock.getBinder())).thenReturn(JudicialUserRoleType.class);

        headerValidationProcessor.process(exchangeMock);
    }
}
