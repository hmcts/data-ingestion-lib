package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.domain.CommonCsvField;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CommonCsvFieldProcessor implements Processor {


    /**
     * Processes the message exchange and set the row id for the record(s).
     * @param exchange the message exchange
     */
    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        AtomicInteger counter = new AtomicInteger(1);
        if (exchange.getIn().getBody() instanceof List) {
            List<CommonCsvField> body = (List<CommonCsvField>) exchange.getIn().getBody();
            body.forEach(i -> i.setRowId((long) counter.getAndIncrement())
            );
            exchange.getMessage().setBody(body);
        } else {
            CommonCsvField body = (CommonCsvField) exchange.getIn().getBody();
            // setting row id to 1 because there is only one record
            body.setRowId(1L);
            exchange.getMessage().setBody(body);
        }
    }
}
