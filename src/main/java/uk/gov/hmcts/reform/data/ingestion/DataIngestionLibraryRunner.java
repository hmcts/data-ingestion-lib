package uk.gov.hmcts.reform.data.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;

import static java.lang.Boolean.FALSE;

@Slf4j
@Component
public class DataIngestionLibraryRunner {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    AuditServiceImpl auditServiceImpl;

    @Value("${idempotent-flag-ingestion}")
    boolean isIdempotentFlagEnabled;

    public void run(Job job, JobParameters params) throws Exception {

        if (isIdempotentFlagEnabled) {
            if (FALSE.equals(auditServiceImpl.isAuditingCompleted())) {
                log.info("Data Ingestion Library running first time for a day::");
                jobLauncher.run(job, params);
                log.info("Data Ingestion Library job run completed::");
            } else {
                log.info("no run of Data Ingestion Library as it has ran for the day::");
            }
            return;
        }
        jobLauncher.run(job, params);
    }

}
