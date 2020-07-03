package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ERROR_MESSAGE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FAILURE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_EXCEPTION_HANDLED;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_STATUS;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.service.EmailService;

@Component
@Slf4j
public class ExceptionProcessor implements Processor {

    @Autowired
    CamelContext camelContext;

    @Autowired
    EmailService emailService;

    @Value("${logging-component-name:''}")
    private String logComponentName;

    @Override
    public void process(Exchange exchange) throws Exception {

        if (isNull(exchange.getContext().getGlobalOptions().get(IS_EXCEPTION_HANDLED))) {
            Map<String, String> globalOptions = exchange.getContext().getGlobalOptions();
            Exception exception = (Exception) exchange.getProperty(EXCEPTION_CAUGHT);
            log.error("{}:: exception in route for data processing:: {}", logComponentName, getStackTrace(exception));
            RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
            globalOptions.put(SCHEDULER_STATUS, FAILURE);
            globalOptions.put(IS_EXCEPTION_HANDLED, TRUE.toString());
            globalOptions.put(ERROR_MESSAGE, exception.getMessage());
            globalOptions.put(FILE_NAME, routeProperties.getFileName());
            throw exception;
        }
    }
}
