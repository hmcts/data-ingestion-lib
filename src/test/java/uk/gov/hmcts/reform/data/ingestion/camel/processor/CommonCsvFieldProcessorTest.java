package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.data.ingestion.camel.domain.CommonCsvField;

import java.util.List;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class CommonCsvFieldProcessorTest {

    Exchange exchangeMock = mock(Exchange.class);
    Message messageMock = mock(Message.class);
    CamelContext camelContext = mock(CamelContext.class);
    CommonCsvFieldProcessor commonCsvFieldProcessor = spy(new CommonCsvFieldProcessor());

    @BeforeEach
    void setUp() {
        when(exchangeMock.getContext()).thenReturn(camelContext);
        when(exchangeMock.getIn()).thenReturn(messageMock);
        when(exchangeMock.getMessage()).thenReturn(messageMock);
    }

    @Test
    void testCommonCsvFieldContainingMutipleRecords() {
        CommonCsvField commonCsvField = new CommonCsvField();
        when(exchangeMock.getIn().getBody()).thenReturn(List.of(commonCsvField));
        commonCsvFieldProcessor.process(exchangeMock);

        verify(commonCsvFieldProcessor).process(exchangeMock);

    }

    @Test
    void testCommonCsvFieldContainingSingleRecord() {
        CommonCsvField commonCsvField = new CommonCsvField();
        when(exchangeMock.getIn().getBody()).thenReturn(commonCsvField);
        commonCsvFieldProcessor.process(exchangeMock);

        verify(commonCsvFieldProcessor).process(exchangeMock);

    }
}