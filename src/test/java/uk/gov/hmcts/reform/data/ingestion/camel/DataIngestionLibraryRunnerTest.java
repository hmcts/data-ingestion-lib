package uk.gov.hmcts.reform.data.ingestion.camel;

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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.powermock.api.mockito.PowerMockito.when;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.DIRECT_JRD;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.START_ROUTE;

public class DataIngestionLibraryRunnerTest {

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

    @InjectMocks
    DataIngestionLibraryRunner dataIngestionLibraryRunner;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void runWhenAuditingCompleteIsFalseTest() throws Exception {
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(false);
        dataIngestionLibraryRunner.run(jobMock, paramsMock);
        verify(jobLauncherMock, times(1)).run(jobMock, paramsMock);
    }

    @Test
    public void runWhenAuditingCompleteIsTrueTest() throws Exception {
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(true);
        dataIngestionLibraryRunner.run(jobMock, paramsMock);
    }

    @Test
    public void runWhenAuditingCompletedPrevDayIsTrueTest() throws Exception {
        ReflectionTestUtils.setField(dataIngestionLibraryRunner, "isIdempotentFlagEnabled", true);
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(false);
        when(auditServiceImpl.isAuditingCompletedPrevDay()).thenReturn(true);
        final JobParameters params = new JobParametersBuilder()
                .addString(START_ROUTE, DIRECT_JRD)
                .toJobParameters();
        dataIngestionLibraryRunner.run(jobMock, params);
        verify(jobLauncherMock, times(0)).run(jobMock, params);
    }

    @Test
    public void runWhenAuditingCompletedCurrentDayIsTrueTest() throws Exception {
        ReflectionTestUtils.setField(dataIngestionLibraryRunner, "isIdempotentFlagEnabled", true);
        when(auditServiceImpl.isAuditingCompleted()).thenReturn(true);
        when(auditServiceImpl.isAuditingCompletedPrevDay()).thenReturn(false);
        final JobParameters params = new JobParametersBuilder()
                .addString(START_ROUTE, DIRECT_JRD)
                .toJobParameters();
        dataIngestionLibraryRunner.run(jobMock, params);
        verify(jobLauncherMock, times(0)).run(jobMock, params);
    }

    @Test
    public void runWhenIdempotentIsFalse() throws Exception {
        dataIngestionLibraryRunner.run(jobMock, paramsMock);
        verify(jobLauncherMock, times(1)).run(jobMock, paramsMock);
    }
}
