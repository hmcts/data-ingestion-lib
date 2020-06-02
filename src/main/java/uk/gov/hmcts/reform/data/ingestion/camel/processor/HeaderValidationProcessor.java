package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import com.opencsv.CSVReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants;

@Component
public class HeaderValidationProcessor implements Processor {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    CamelContext camelContext;


    @Override
    public void process(Exchange exchange) throws Exception {

        RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(MappingConstants.ROUTE_DETAILS);
        String csv = exchange.getIn().getBody(String.class);
        CSVReader reader = new CSVReader(new StringReader(csv));
        String[] header = reader.readNext();
        Field[] allFields = applicationContext.getBean(routeProperties.getBinder())
                .getClass().getDeclaredFields();
        List<Field> csvFields = new ArrayList<>();

        for (Field field : allFields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                csvFields.add(field);
            }
        }

        //Auditing in database if headers are missing
        if (header.length > csvFields.size()) {
            exchange.getIn().setHeader(MappingConstants.HEADER_EXCEPTION, MappingConstants.HEADER_EXCEPTION);
            camelContext.getGlobalOptions().put(MappingConstants.FILE_NAME, routeProperties.getFileName());
            throw new RouteFailedException("Mismatch headers in csv for ::" + routeProperties.getFileName());
        }

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(Charset.forName("UTF-8")));

        exchange.getMessage().setBody(inputStream);
    }
}
