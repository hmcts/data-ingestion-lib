package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.util.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.FileStatus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.data.ingestion.camel.helper.JrdTestSupport.createRoutePropertiesMock;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;

public class FileResponseProcessorTest {

    Exchange exchangeMock = mock(Exchange.class);

    FileStatus fileStatus = mock(FileStatus.class);

    CamelContext camelContext = spy(new DefaultCamelContext());

    FileResponseProcessor fileResponseProcessor = spy(new FileResponseProcessor());

    Message messageMock = mock(Message.class);

    ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

    @BeforeEach
    @SneakyThrows
    public void setUp() {
        ReflectionUtils.setField(fileResponseProcessor.getClass()
            .getDeclaredField("applicationContext"), fileResponseProcessor, applicationContext);
        when(exchangeMock.getContext()).thenReturn(camelContext);
        when(exchangeMock.getIn()).thenReturn(messageMock);
        when(messageMock.getHeader(ROUTE_DETAILS)).thenReturn(createRoutePropertiesMock());
        when(applicationContext.getBeanFactory()).thenReturn(mock(ConfigurableListableBeanFactory.class));
    }

    @SneakyThrows
    @Test
    public void testProcess() {
        fileResponseProcessor.process(exchangeMock);
        verify(fileResponseProcessor).process(exchangeMock);
    }
}
