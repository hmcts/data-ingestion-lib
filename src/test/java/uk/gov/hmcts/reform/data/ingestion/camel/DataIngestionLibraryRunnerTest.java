package uk.gov.hmcts.reform.data.ingestion.camel;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.data.ingestion.DataIngestionLibraryRunner;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    CloudStorageAccount cloudStorageAccount;

    @Mock
    AzureBlobConfig azureBlobConfig;

    @Mock
    CloudBlobClient blobClient;

    @Mock
    CloudBlobContainer container;

    @Mock
    CloudBlockBlob cloudBlockBlob;

    @Mock
    BlobProperties blobProperties;

    @InjectMocks
    DataIngestionLibraryRunner dataIngestionLibraryRunner;

    @BeforeEach
    void setUp() throws Exception {
        openMocks(this);
        when(cloudStorageAccount.createCloudBlobClient()).thenReturn(blobClient);
        when(azureBlobConfig.getContainerName()).thenReturn("test");
        when(blobClient.getContainerReference(anyString())).thenReturn(container);
        when(container.getBlockBlobReference(any())).thenReturn(cloudBlockBlob);
        when(cloudBlockBlob.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getLastModified()).thenReturn(new Date());
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
