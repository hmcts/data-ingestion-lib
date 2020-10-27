package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;

import java.util.Date;

import static org.apache.commons.lang.time.DateUtils.isSameDay;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus.NEW;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus.NOT_EXISTS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus.STALE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.BLOBPATH;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_FILE_STALE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.NOT_STALE_FILE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;

/**
 * This FileReadProcessor checks if file has following status
 * Stale: old File then do nothing and log error in Audit table
 * Not-exist in blob then do nothing and log error in Audit table
 * New File for today's date the consumes the file and stores in Message body.
 *
 * @since 2020-10-27
 */
@Slf4j
@Component
public class FileReadProcessor implements Processor {

    @Value("${file-read-time-out}")
    private int fileReadTimeOut;

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    @Autowired
    private AzureBlobConfig azureBlobConfig;

    @Autowired
    private AuditServiceImpl auditService;

    private Date fileTimeStamp;

    @Autowired
    @Qualifier("credscloudStorageAccount")
    private CloudStorageAccount cloudStorageAccount;

    /**
     * Consumes files only if it is not Stale and stores it in message body.
     *
     * @param exchange Exchange
     */
    @Override
    public void process(Exchange exchange) {
        log.info("{}:: FileReadProcessor starts::", logComponentName);
        final String blobFilePath = (String) exchange.getProperty(BLOBPATH);
        final CamelContext context = exchange.getContext();
        final ConsumerTemplate consumer = context.createConsumerTemplate();
        RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
        String fileName = routeProperties.getFileName();
        String schedulerTime = context.getGlobalOption(SCHEDULER_START_TIME);

        //Check Stale OR Not existing file and exit run with proper error message
        if (getFileStatusInBlobContainer(fileName, schedulerTime).equals(STALE)) {
            exchange.getMessage().setHeader(IS_FILE_STALE, true);
            auditService.auditException(exchange.getContext(), String.format(
                "%s file with timestamp %s not loaded due to file stale error",
                fileName,
                DateFormatUtils.format(fileTimeStamp, "yyyy-MM-dd HH:mm:SS")));
            return;
        } else if (getFileStatusInBlobContainer(fileName, schedulerTime).equals(NOT_EXISTS)) {
            auditService.auditException(exchange.getContext(), String.format(
                "%s file is not exists in container", routeProperties.getFileName()));
            return;
        }

        exchange.getMessage().setHeader(IS_FILE_STALE, false);
        context.getGlobalOptions().put(fileName, NOT_STALE_FILE);
        exchange.getMessage().setBody(consumer.receiveBody(blobFilePath, fileReadTimeOut));
    }

    /**
     * Connects to Blob storage and checks if file timestamp is stale.
     *
     * @param fileName for getting timestamp of
     * @return is File TimeStamp Stale
     */
    private BlobStatus getFileStatusInBlobContainer(String fileName, String schedulerTime) {
        try {

            CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
            CloudBlobContainer container =
                blobClient.getContainerReference(azureBlobConfig.getContainerName());

            CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(fileName);

            if (cloudBlockBlob.exists()) {
                cloudBlockBlob.downloadAttributes();
                fileTimeStamp = cloudBlockBlob.getProperties().getCreatedTime();
                return (isSameDay(fileTimeStamp, new Date(Long.valueOf(schedulerTime)))) ? NEW : STALE;
            }

            return NOT_EXISTS;
        } catch (Exception exp) {
            log.error("{}:: Failed to get file timestamp :: ", logComponentName, exp);
            throw new RouteFailedException("Failed to get file timestamp ::");
        }
    }
}
