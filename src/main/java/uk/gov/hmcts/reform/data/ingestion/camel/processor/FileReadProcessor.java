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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;

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
    CloudStorageAccount cloudStorageAccount;

    @Override
    public void process(Exchange exchange) throws Exception {
        log.info("{}:: FileReadProcessor starts::", logComponentName);
        String blobFilePath = (String) exchange.getProperty(BLOBPATH);
        CamelContext context = exchange.getContext();
        ConsumerTemplate consumer = context.createConsumerTemplate();
        RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
        String fileName = routeProperties.getFileName();

        //Check Stale OR Not existing file and exit run with proper error message
        if (getFileStatusInBlobContainer(fileName).equals(STALE)) {
            exchange.getMessage().setHeader(IS_FILE_STALE, true);
            auditService.auditException(exchange.getContext(), String.format(
                "%s file with timestamp %s not loaded due to file stale error",
                fileName,
                DateFormatUtils.format(fileTimeStamp, "yyyy-MM-dd HH:mm:SS")));
            return;
        } else if (getFileStatusInBlobContainer(fileName).equals(NOT_EXISTS)) {
            auditService.auditException(exchange.getContext(), String.format(
                "%s file is not exists in container", routeProperties.getFileName()));
            return;
        }

        exchange.getMessage().setHeader(IS_FILE_STALE, false);
        exchange.getMessage().setBody(consumer.receiveBody(blobFilePath, fileReadTimeOut));
    }

    /**
     * Connects to Blob storage and checks if file timestamp is stale.
     *
     * @param fileName for getting timestamp of
     * @return is File TimeStamp Stale
     */
    private BlobStatus getFileStatusInBlobContainer(String fileName) throws Exception {
        BlobStatus blobStatus = NEW;
        try {
            CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
            CloudBlobContainer container =
                blobClient.getContainerReference(azureBlobConfig.getContainerName());

            CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(fileName);

            if (cloudBlockBlob.exists()) {
                cloudBlockBlob.downloadAttributes();
                fileTimeStamp = cloudBlockBlob.getProperties().getCreatedTime();
                return (isSameDay(fileTimeStamp, new Date())) ? STALE : NEW;
            }
            return NOT_EXISTS;
        } catch (Exception exp) {
            log.error("{}:: Failed to get file timestamp :: ", logComponentName, exp);
            throw exp;
        }
    }
}
