package uk.gov.hmcts.reform.data.ingestion;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;

import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_START_ROUTE_JRD;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_NOT_BLANK;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.START_ROUTE;

/**
 * This DataIngestionLibraryRunner Triggers spring batch job started by consumer LRD/JRD
 * Also it handle idempotent logic (Runs daily once and ignore second run for the day).
 *
 * @since 2020-10-27
 */
@Slf4j
@Component
public class DataIngestionLibraryRunner {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    protected AuditServiceImpl auditServiceImpl;

    @Value("${idempotent-flag-ingestion}")
    boolean isIdempotentFlagEnabled;

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    @Autowired
    protected CamelContext camelContext;

    @Autowired
    private AzureBlobConfig azureBlobConfig;

    @Autowired
    private BlobServiceClientBuilder blobServiceClientBuilder;

    @Value("${route.judicial-user-profile-orchestration.file-name:Personal}")
    protected String fileName;

    public void run(Job job, JobParameters params) throws Exception {
        Optional<Date> fileTimestamp = getFileTimestamp(fileName);

        if (isIdempotentFlagEnabled
                && ((isStartRouteJRD(params) && auditingCompletedTodayOrPrevDay(auditServiceImpl, fileTimestamp))
                        || isAuditingCompleted.test(auditServiceImpl))) {

            log.info("{}:: no run of Data Ingestion Library as it has ran for the day::", logComponentName);
            return;
        }

        log.info("{}:: Data Ingestion Library starts::", logComponentName);
        jobLauncher.run(job, params);
        log.info("{}:: Data Ingestion Library job run completed::", logComponentName);
    }

    protected Optional<Date> getFileTimestamp(String fileName) throws URISyntaxException {
        camelContext.getGlobalOptions()
            .put(SCHEDULER_START_TIME, String.valueOf(new Date().getTime()));

        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(
                azureBlobConfig.getAccountName(), azureBlobConfig.getAccountKey());
        String uri = String.format("https://%s.blob.core.windows.net", azureBlobConfig.getAccountName());

        BlobServiceClient blobClient = blobServiceClientBuilder
                .endpoint(uri)
                .credential(credential)
                .buildClient();
        camelContext.getRegistry().bind("client", blobClient);

        BlobContainerClient blobContainerClient = blobClient.getBlobContainerClient(
                azureBlobConfig.getContainerName());

        BlobClient cloudBlockBlob = blobContainerClient.getBlobClient(fileName);

        Optional<Date> fileTimestamp = Optional.empty();

        if (cloudBlockBlob.exists()) {
            OffsetDateTime lastModified = cloudBlockBlob.getProperties().getLastModified();
            Date lastModifiedDate = new Date(lastModified.toInstant().toEpochMilli());
            fileTimestamp = Optional.ofNullable(lastModifiedDate);
        }
        return fileTimestamp;
    }

    private boolean isStartRouteJRD(JobParameters params) {
        return IS_NOT_BLANK.and(IS_START_ROUTE_JRD).test(params.getString(START_ROUTE));
    }

    private boolean auditingCompletedTodayOrPrevDay(AuditServiceImpl auditServiceImpl, Optional<Date> fileTimeStamp) {
        return isAuditingCompleted.test(auditServiceImpl)
                || isAuditingCompletedPrevDay.test(auditServiceImpl, fileTimeStamp);
    }

    public static final Predicate<AuditServiceImpl> isAuditingCompleted =
            AuditServiceImpl::isAuditingCompleted;

    public static final BiPredicate<AuditServiceImpl, Optional<Date>> isAuditingCompletedPrevDay =
            AuditServiceImpl::isAuditingCompletedPrevDay;
}
