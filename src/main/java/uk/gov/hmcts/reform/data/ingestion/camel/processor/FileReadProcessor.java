package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.function.BiPredicate;

import static net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang.time.DateUtils.isSameDay;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus.NEW;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus.NOT_EXISTS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.BlobStatus.STALE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.BLOBPATH;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_FILE_STALE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.STALE_FILE_ERROR;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FILE_NOT_EXISTS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.NOT_STALE_FILE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_NOT_BLANK;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_START_ROUTE_JRD;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.MILLIS_IN_A_DAY;

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

    @Autowired
    private BlobServiceClientBuilder blobServiceClientBuilder;
    private Date fileTimeStamp;

    private BlobServiceClient blobClient;

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

        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(
                azureBlobConfig.getAccountName(), azureBlobConfig.getAccountKey());
        String uri = String.format("https://%s.blob.core.windows.net", azureBlobConfig.getAccountName());

        blobClient = blobServiceClientBuilder
                .endpoint(uri)
                .credential(credential)
                .buildClient();
        context.getRegistry().bind("client", blobClient);

        final ConsumerTemplate consumer = context.createConsumerTemplate();
        RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
        String fileName = routeProperties.getFileName();
        String schedulerTime = context.getGlobalOption(SCHEDULER_START_TIME);

        //Check Stale OR Not existing file and exit run with proper error message
        if (getFileStatusInBlobContainer(routeProperties, schedulerTime).equals(STALE)) {
            exchange.getMessage().setHeader(IS_FILE_STALE, true);
            throw new RouteFailedException(String.format(STALE_FILE_ERROR,
                fileName, DateFormatUtils.format(fileTimeStamp, "yyyy-MM-dd HH:mm:SS")));
        } else if (getFileStatusInBlobContainer(routeProperties, schedulerTime).equals(NOT_EXISTS)) {
            throw new RouteFailedException(String.format(FILE_NOT_EXISTS,
                routeProperties.getFileName()));
        }

        exchange.getMessage().setHeader(IS_FILE_STALE, false);
        context.getGlobalOptions().put(fileName, NOT_STALE_FILE);
        exchange.getMessage().setBody(consumer.receiveBody(blobFilePath, fileReadTimeOut));
    }

    /**
     * Connects to Blob storage and checks if file timestamp is stale.
     *
     * @param routeProperties for getting fileName and start route
     * @param schedulerTime time when the scheduler starts
     * @return is File TimeStamp Stale
     */
    private BlobStatus getFileStatusInBlobContainer(RouteProperties routeProperties, String schedulerTime) {
        try {
            BlobContainerClient blobContainerClient = blobClient.getBlobContainerClient(
                    azureBlobConfig.getContainerName());

            BlobClient cloudBlockBlob = blobContainerClient.getBlobClient(routeProperties.getFileName());

            //if scheduler time not set via camel context then set as current date else camel context pass current
            // data time
            if (isEmpty(schedulerTime)) {
                schedulerTime = String.valueOf(new Date(System.currentTimeMillis()).getTime());
            }

            if (cloudBlockBlob.exists()) {
                OffsetDateTime lastModified = cloudBlockBlob.getProperties().getLastModified();
                fileTimeStamp = new Date(lastModified.toInstant().toEpochMilli());

                BlobStatus blobStatus;
                Date today = new Date(Long.parseLong(schedulerTime));

                if (IS_NOT_BLANK.and(IS_START_ROUTE_JRD).test(routeProperties.getStartRoute())) {
                    blobStatus = isSameDay.or(isPrevDay).test(fileTimeStamp, today)
                            ? NEW
                            : STALE;
                } else {
                    blobStatus = isSameDay.test(fileTimeStamp, today) ? NEW : STALE;
                }

                return blobStatus;
            }

            return NOT_EXISTS;
        } catch (Exception exp) {
            log.error("{}:: Failed to get file timestamp :: ", logComponentName, exp);
            throw new RouteFailedException("Failed to get file timestamp ::");
        }
    }

    public static final BiPredicate<Date, Date> isSameDay = DateUtils::isSameDay;
    public static final BiPredicate<Date, Date> isPrevDay = (fileTS, schedulerDate) ->
        isSameDay(fileTS, new Date(schedulerDate.getTime() - MILLIS_IN_A_DAY));
}
