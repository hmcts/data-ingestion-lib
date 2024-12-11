package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.common.StorageSharedKeyCredential;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.Registry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.apache.camel.spring.util.ReflectionUtils.setField;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.BLOBPATH;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.MILLIS_IN_A_DAY;

public class FileReaderTest {

    @Mock
    Exchange exchangeMock;
    @Mock
    Message messageMock;
    @Mock
    RouteProperties routePropertiesMock;
    @Mock
    CamelContext camelContext;
    FileReadProcessor fileReadProcessor = spy(new FileReadProcessor());
    @Mock
    AzureBlobConfig azureBlobConfig;
    BlobServiceClient blobClient = PowerMockito.mock(BlobServiceClient.class);
    BlobContainerClient container = PowerMockito.mock(BlobContainerClient.class);
    @Mock
    BlobClient cloudBlockBlob;
    BlobProperties blobProperties = PowerMockito.mock(BlobProperties.class);
    @Mock
    AuditServiceImpl auditService;
    @Mock
    ConsumerTemplate consumerTemplate;


    @BeforeEach
    public void setUp() throws Exception {
        openMocks(this);

        when(exchangeMock.getIn()).thenReturn(messageMock);
        when(exchangeMock.getIn().getHeader(ROUTE_DETAILS)).thenReturn(routePropertiesMock);
        when(exchangeMock.getProperty(BLOBPATH)).thenReturn("blobpath");
        when(exchangeMock.getContext()).thenReturn(camelContext);
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(camelContext.getRegistry()).thenReturn(mock(Registry.class));
        when(routePropertiesMock.getFileName()).thenReturn("test-file");
        setField(fileReadProcessor.getClass()
            .getDeclaredField("azureBlobConfig"), fileReadProcessor, azureBlobConfig);
        when(azureBlobConfig.getAccountKey()).thenReturn("key");
        when(azureBlobConfig.getAccountName()).thenReturn("accountName");
        setField(fileReadProcessor.getClass()
            .getDeclaredField("auditService"), fileReadProcessor, auditService);
        setField(fileReadProcessor.getClass()
            .getDeclaredField("azureBlobConfig"), fileReadProcessor, azureBlobConfig);
        when(azureBlobConfig.getContainerName()).thenReturn("test");
        when(cloudBlockBlob.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getLastModified()).thenReturn(OffsetDateTime.now());
        when(blobClient.getBlobContainerClient(anyString())).thenReturn(container);
        when(container.getBlobClient(any())).thenReturn(cloudBlockBlob);
        when(exchangeMock.getContext().createConsumerTemplate()).thenReturn(consumerTemplate);
        when(camelContext.getGlobalOption(SCHEDULER_START_TIME)).thenReturn(String.valueOf(new Date().getTime()));

        BlobServiceClientBuilder mockBuilder = PowerMockito.mock(BlobServiceClientBuilder.class);
        when(mockBuilder.endpoint(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.credential(any(StorageSharedKeyCredential.class))).thenReturn(mockBuilder);
        when(mockBuilder.buildClient()).thenReturn(blobClient);
        setField(fileReadProcessor.getClass()
                .getDeclaredField("blobServiceClientBuilder"), fileReadProcessor, mockBuilder);
    }

    @Test
    @SneakyThrows
    public void testProcessStaleFile() {
        when(cloudBlockBlob.exists()).thenReturn(true);
        doNothing().when(auditService).auditException(any(), any());
        fileReadProcessor.process(exchangeMock);
        verify(fileReadProcessor).process(exchangeMock);
    }

    @Test
    @SneakyThrows
    void testProcessStaleFileForJRDWhenAuditingCompletedOnPreviousDay() {
        when(cloudBlockBlob.exists()).thenReturn(true);
        doNothing().when(auditService).auditException(any(), any());
        when(routePropertiesMock.getStartRoute()).thenReturn("direct:JRD");
        when(blobProperties.getLastModified()).thenReturn(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(new Date().getTime() - MILLIS_IN_A_DAY), ZoneId.systemDefault()));
        fileReadProcessor.process(exchangeMock);
        verify(fileReadProcessor).process(exchangeMock);
    }

    @Test
    @SneakyThrows
    void testProcessStaleFileForJRDWhenAuditingCompletedOnCurrentDay() {
        when(cloudBlockBlob.exists()).thenReturn(true);
        doNothing().when(auditService).auditException(any(), any());
        when(routePropertiesMock.getStartRoute()).thenReturn("direct:JRD");
        fileReadProcessor.process(exchangeMock);
        verify(fileReadProcessor).process(exchangeMock);
    }

    @Test
    @SneakyThrows
    public void testProcessNonExistFile() {
        when(cloudBlockBlob.exists()).thenReturn(false);
        doNothing().when(auditService).auditException(any(), any());
        assertThrows(RouteFailedException.class, () -> fileReadProcessor.process(exchangeMock));
        verify(fileReadProcessor).process(exchangeMock);
    }

    @Test
    @SneakyThrows
    public void testProcessNewFile() {
        when(blobProperties.getLastModified()).thenReturn(OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli(new Date().getTime() - 1 * 24 * 60 * 60 * 1000), ZoneId.systemDefault()));
        when(cloudBlockBlob.exists()).thenReturn(true);
        doNothing().when(auditService).auditException(any(), any());
        when(consumerTemplate.receiveBody(anyString(), anyInt())).thenReturn("testbody");
        assertThrows(RouteFailedException.class, () -> fileReadProcessor.process(exchangeMock));
        verify(fileReadProcessor).process(exchangeMock);
    }

    @Test
    @SneakyThrows
    public void testProcessFileExceptsException() {
        when(cloudBlockBlob.exists()).thenThrow(new RouteFailedException("invalid cloud account"));
        assertThrows(RouteFailedException.class, () -> fileReadProcessor.process(exchangeMock));
        verify(fileReadProcessor).process(exchangeMock);
    }
}
