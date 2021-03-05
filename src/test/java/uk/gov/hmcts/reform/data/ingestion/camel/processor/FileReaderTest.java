package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;

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
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.BLOBPATH;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;

public class FileReaderTest {

    Exchange exchangeMock = mock(Exchange.class);
    Message messageMock = mock(Message.class);
    RouteProperties routePropertiesMock = mock(RouteProperties.class);
    CamelContext camelContext = mock(CamelContext.class);
    FileReadProcessor fileReadProcessor = spy(new FileReadProcessor());
    AzureBlobConfig azureBlobConfig = mock(AzureBlobConfig.class);
    CloudBlobClient blobClient = mock(CloudBlobClient.class);
    CloudStorageAccount cloudStorageAccount = mock(CloudStorageAccount.class);
    CloudBlobContainer container = mock(CloudBlobContainer.class);
    CloudBlockBlob cloudBlockBlob = mock(CloudBlockBlob.class);
    BlobProperties blobProperties = mock(BlobProperties.class);
    AuditServiceImpl auditService = mock(AuditServiceImpl.class);
    ConsumerTemplate consumerTemplate = mock(ConsumerTemplate.class);


    @Before
    public void setUp() throws Exception {
        when(exchangeMock.getIn()).thenReturn(messageMock);
        when(exchangeMock.getIn().getHeader(ROUTE_DETAILS)).thenReturn(routePropertiesMock);
        when(exchangeMock.getProperty(BLOBPATH)).thenReturn("blobpath");
        when(exchangeMock.getContext()).thenReturn(camelContext);
        when(exchangeMock.getMessage()).thenReturn(messageMock);
        when(routePropertiesMock.getFileName()).thenReturn("test-file");
        setField(fileReadProcessor.getClass()
            .getDeclaredField("azureBlobConfig"), fileReadProcessor, azureBlobConfig);
        when(azureBlobConfig.getAccountKey()).thenReturn("key");
        when(azureBlobConfig.getAccountName()).thenReturn("accountName");
        setField(fileReadProcessor.getClass()
            .getDeclaredField("auditService"), fileReadProcessor, auditService);
        setField(fileReadProcessor.getClass()
            .getDeclaredField("cloudStorageAccount"), fileReadProcessor, cloudStorageAccount);
        setField(fileReadProcessor.getClass()
            .getDeclaredField("azureBlobConfig"), fileReadProcessor, azureBlobConfig);
        when(azureBlobConfig.getContainerName()).thenReturn("test");
        when(cloudBlockBlob.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getLastModified()).thenReturn(new Date());
        when(cloudStorageAccount.createCloudBlobClient()).thenReturn(blobClient);
        when(blobClient.getContainerReference(anyString())).thenReturn(container);
        when(container.getBlockBlobReference(any())).thenReturn(cloudBlockBlob);
        when(exchangeMock.getContext().createConsumerTemplate()).thenReturn(consumerTemplate);
        when(camelContext.getGlobalOption(SCHEDULER_START_TIME)).thenReturn(String.valueOf(new Date().getTime()));
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
    public void testProcessNonExistFile() {
        when(cloudBlockBlob.exists()).thenReturn(false);
        doNothing().when(auditService).auditException(any(), any());
        assertThrows(RouteFailedException.class, () -> fileReadProcessor.process(exchangeMock));
        verify(fileReadProcessor).process(exchangeMock);
    }

    @Test
    @SneakyThrows
    public void testProcessNewFile() {
        when(blobProperties.getLastModified()).thenReturn(new Date(
            new Date().getTime() - 1 * 24 * 60 * 60 * 1000));
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
