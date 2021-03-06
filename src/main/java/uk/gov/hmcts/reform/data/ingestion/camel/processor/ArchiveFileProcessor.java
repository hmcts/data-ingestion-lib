package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This ArchiveFileProcessor consumes file from blob storage
 * And set in Message body of camel context which will be use
 * by ArchivalRoute of camel to copy blob in archive blob folder.
 *
 * @since 2020-10-27
 */
@Component
public class ArchiveFileProcessor implements Processor {

    @Value("${archival-file-names}")
    List<String> archivalFileNames;

    @Value("${active-blob-path}")
    String activeBlobs;

    @Value("${archival-cred}")
    String archivalCred;

    @Value("${archival-date-format}")
    String archivalDateFormat;

    @Value("${file-read-time-out}")
    int fileReadTimeOut;


    /**
     * Consumes Blob file form Azure Storage endpoint
     * And store in Message body.
     *
     * @param exchange Exchange
     */
    @Override
    public void process(Exchange exchange) {

        Integer count = exchange.getProperty("CamelLoopIndex", Integer.class);
        String date = new SimpleDateFormat(archivalDateFormat).format(new Date());
        String fileName = archivalFileNames.get(count);
        exchange.getIn().setHeader("filename", "/" + fileName.concat(date));
        CamelContext context = exchange.getContext();
        ConsumerTemplate consumer = context.createConsumerTemplate();
        exchange.getMessage().setBody(consumer.receiveBody(activeBlobs + "/" + fileName
            + "?" + archivalCred, fileReadTimeOut));
    }
}
