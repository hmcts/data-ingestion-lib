package uk.gov.hmcts.reform.data.ingestion.camel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import uk.gov.hmcts.reform.data.ingestion.DataIngestionLibraryRunner;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

public class DataIngestionLibraryRunnerTest {

    @Mock
    AuditServiceImpl auditServiceImpl;
    @Mock
    JobParameters paramsMock;
    @Mock
    Job jobMock;
    @Mock
    JobLauncher jobLauncherMock;

    @InjectMocks
    DataIngestionLibraryRunner dataIngestionLibraryRunner;

    @Before
    public void setUp() {
        initMocks(this);
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
    public void runWhenIdempotentIsFalse() throws Exception {
        dataIngestionLibraryRunner.run(jobMock, paramsMock);
        verify(jobLauncherMock, times(1)).run(jobMock, paramsMock);
    }
}
