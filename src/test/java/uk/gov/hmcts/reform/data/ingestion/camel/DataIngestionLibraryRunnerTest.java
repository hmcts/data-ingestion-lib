package uk.gov.hmcts.reform.data.ingestion.camel;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.data.ingestion.DataIngestionLibraryRunner;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.powermock.api.mockito.PowerMockito.when;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.DIRECT_JRD;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.START_ROUTE;

class DataIngestionLibraryRunnerTest {

    @Mock
    AuditServiceImpl auditServiceImpl;
    @Mock
    JobParameters paramsMock;
    @Mock
    Job jobMock;
    @Mock
    JobLauncher jobLauncherMock;

    @Mock
    CamelContext camelContext;

    @Mock
    AzureBlobConfig azureBlobConfig;

    @Mock
    BlobServiceClient blobClient;

    @Mock
    BlobContainerClient container;

    @Mock
    BlobClient cloudBlockBlob;

    @Mock
    BlobProperties blobProperties;

    @InjectMocks
    DataIngestionLibraryRunner dataIngestionLibraryRunner;

    @BeforeEach
    void setUp() throws Exception {
        openMocks(this);
        Mockito.when(camelContext.getRegistry()).thenReturn(mock(Registry.class));
        when(azureBlobConfig.getContainerName()).thenReturn("test");
        when(azureBlobConfig.getAccountName()).thenReturn("test-account-name");
        when(azureBlobConfig.getAccountKey()).thenReturn(
                new String(Base64.getEncoder().encode("test-account-key".getBytes())));
        when(blobClient.getBlobContainerClient(anyString())).thenReturn(container);
        when(container.getBlobClient(any())).thenReturn(cloudBlockBlob);
        when(cloudBlockBlob.getProperties()).thenReturn(blobProperties);
        when(cloudBlockBlob.exists()).thenReturn(true);
        when(blobProperties.getLastModified()).thenReturn(OffsetDateTime.now());
    }

    @Test
    void runWhenAuditingCompleteIsFalseTest() throws Exception {
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(false);
        dataIngestionLibraryRunner.run(jobMock, paramsMock);
        verify(jobLauncherMock, times(1)).run(jobMock, paramsMock);
    }

    @Test
    void runWhenAuditingCompleteIsTrueTest() throws Exception {
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(true);
        dataIngestionLibraryRunner.run(jobMock, paramsMock);
    }

    @Test
    void runWhenAuditingCompletedPrevDayIsTrueTest() throws Exception {
        ReflectionTestUtils.setField(dataIngestionLibraryRunner, "isIdempotentFlagEnabled", true);
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(false);
        when(auditServiceImpl.isAuditingCompletedPrevDay(any())).thenReturn(true);

        final JobParameters params = new JobParametersBuilder()
                .addString(START_ROUTE, DIRECT_JRD)
                .toJobParameters();
        dataIngestionLibraryRunner.run(jobMock, params);
        verify(jobLauncherMock, times(0)).run(jobMock, params);
    }

    @Test
    void runWhenAuditingCompletedCurrentDayIsTrueTest() throws Exception {
        ReflectionTestUtils.setField(dataIngestionLibraryRunner, "isIdempotentFlagEnabled", true);
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(true);
        when(auditServiceImpl.isAuditingCompletedPrevDay(any())).thenReturn(false);
        final JobParameters params = new JobParametersBuilder()
                .addString(START_ROUTE, DIRECT_JRD)
                .toJobParameters();
        dataIngestionLibraryRunner.run(jobMock, params);
        verify(jobLauncherMock, times(0)).run(jobMock, params);
    }

    @Test
    void runWhenIdempotentIsFalse() throws Exception {
        dataIngestionLibraryRunner.run(jobMock, paramsMock);
        verify(jobLauncherMock, times(1)).run(jobMock, paramsMock);
    }
}
