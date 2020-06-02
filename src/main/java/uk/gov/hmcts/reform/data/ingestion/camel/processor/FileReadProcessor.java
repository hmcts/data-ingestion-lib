package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants;

@Slf4j
@Component
public class FileReadProcessor implements Processor {

    @Value("${file-read-time-out}")
    int fileReadTimeOut;

    @Override
    public void process(Exchange exchange) {
        log.info("::FileReadProcessor starts::");
        String blobFilePath = (String) exchange.getProperty(MappingConstants.BLOBPATH);
        CamelContext context = exchange.getContext();
        ConsumerTemplate consumer = context.createConsumerTemplate();
        exchange.getMessage().setBody(consumer.receiveBody(blobFilePath, fileReadTimeOut));
    }
}
