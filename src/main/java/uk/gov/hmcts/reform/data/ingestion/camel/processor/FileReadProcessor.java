package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.BLOBPATH;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_FILE_STALE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;

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
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.configuration.AzureBlobConfig;
import java.util.Date;

@Slf4j
@Component
public class FileReadProcessor implements Processor {

    @Value("${file-read-time-out}")
    protected int fileReadTimeOut;

    @Value("${logging-component-name:data_ingestion}")
    protected String logComponentName;

    @Autowired
    protected AzureBlobConfig azureBlobConfig;

    @Autowired
    protected AuditServiceImpl auditService;

    protected Date fileTimeStamp;

    @Override
    public void process(Exchange exchange) {
        log.info("{}:: FileReadProcessor starts::", logComponentName);
        String blobFilePath = (String) exchange.getProperty(BLOBPATH);
        CamelContext context = exchange.getContext();
        ConsumerTemplate consumer = context.createConsumerTemplate();
        RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
        String fileName = routeProperties.getFileName();
        if (isFileTimeStampStale(fileName)) {
            exchange.getMessage().setHeader(IS_FILE_STALE, true);
            auditService.auditException(exchange.getContext(), String.format(
                    "%s file with timestamp %s not loaded due to file stale error",
                    routeProperties.getFileName(),
                    DateFormatUtils.format(fileTimeStamp, "yyyy-MM-dd HH:mm:SS")), true);
        } else {
            exchange.getMessage().setHeader(IS_FILE_STALE, false);
            exchange.getMessage().setBody(consumer.receiveBody(blobFilePath, fileReadTimeOut));
        }
    }

    /**
     * Connects to Blob storage and checks if file timestamp is stale.
     *
     * @param fileName for getting timestamp of
     * @return is File TimeStamp Stale
     */
    private boolean isFileTimeStampStale(String fileName) {
        try {
            final String storageConnectionString = "DefaultEndpointsProtocol=https"
                    .concat(";AccountName=" + azureBlobConfig.getAccountName())
                    .concat(";AccountKey=" + azureBlobConfig.getAccountKey());

            final CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
            CloudBlobContainer container =
                    blobClient.getContainerReference(azureBlobConfig.getContainerName());

            CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(fileName);
            cloudBlockBlob.downloadAttributes();
            fileTimeStamp = cloudBlockBlob.getProperties().getCreatedTime();

        } catch (Exception exp) {
            log.error("{}:: Failed to get file timestamp :: {}", logComponentName, exp);
        }
        return isNull(fileTimeStamp) ? true : !DateUtils.isSameDay(fileTimeStamp, new Date());
    }
}
