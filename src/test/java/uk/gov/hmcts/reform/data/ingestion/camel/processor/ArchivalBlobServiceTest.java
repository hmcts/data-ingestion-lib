package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.data.ingestion.camel.route.ArchivalRoute;
import uk.gov.hmcts.reform.data.ingestion.camel.service.ArchivalBlobServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ArchivalBlobServiceTest {

    CamelContext camelContext = spy(DefaultCamelContext.class);

    ArchivalBlobServiceImpl archivalBlobService;

    SimpleRegistry registry = spy(new SimpleRegistry());

    ArchivalRoute archivalRoute = spy(ArchivalRoute.class);

    ProducerTemplate producerTemplate = mock(ProducerTemplate.class);

    @Before
    @SneakyThrows
    public void setUp() {

        archivalBlobService = spy(new ArchivalBlobServiceImpl());
        List<String> fileNames = new ArrayList<>();
        fileNames.add("test");
        fileNames.add("test1");
        setField(archivalBlobService, "archivalFileNames", fileNames);
        setField(archivalBlobService, "camelContext", camelContext);
        setField(archivalBlobService, "archivalRoute", archivalRoute);
        setField(archivalBlobService, "producerTemplate", producerTemplate);
        doNothing().when(archivalRoute).archivalRoute(anyList());
        doNothing().when(producerTemplate).sendBody(any());
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecuteArchiving() {
        archivalBlobService.executeArchiving();
        verify(archivalBlobService).executeArchiving();
    }
}
