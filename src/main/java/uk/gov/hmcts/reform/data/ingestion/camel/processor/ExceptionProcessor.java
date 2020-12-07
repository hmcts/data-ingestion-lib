package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.FileStatus;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.service.AuditServiceImpl;
import uk.gov.hmcts.reform.data.ingestion.camel.service.EmailServiceImpl;

import java.util.Map;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.DataLoadUtil.getFileDetails;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.DataLoadUtil.registerFileStatusBean;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ERROR_MESSAGE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FAILURE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FILE_NAME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_EXCEPTION_HANDLED;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.TABLE_NAME;

/**
 * This ExceptionProcessor gets runtime failures/exceptions
 * while executing dataload-route or archive route
 * And Stores it in camel context which will be use by Consumers
 * LRD/JRD to log those exceptions.
 *
 * @since 2020-10-27
 */
@Component
@Slf4j
public class ExceptionProcessor implements Processor {

    @Autowired
    CamelContext camelContext;

    @Autowired
    EmailServiceImpl emailServiceImpl;

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    @Autowired
    FileResponseProcessor fileResponseProcessor;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    AuditServiceImpl auditService;



    /**
     * Capturing exceptions from routes and storing in camel context.
     *
     * @param exchange Exchange
     * @throws Exception exception
     */
    @Override
    public void process(Exchange exchange) throws Exception {

        if (isNull(exchange.getContext().getGlobalOptions().get(IS_EXCEPTION_HANDLED))) {
            final Map<String, String> globalOptions = exchange.getContext().getGlobalOptions();

            Exception exception = (Exception) exchange.getProperty(EXCEPTION_CAUGHT);
            log.error("{}:: exception in route for data processing:: {}", logComponentName, getStackTrace(exception));
            RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
            FileStatus fileStatus = getFileDetails(exchange.getContext(), routeProperties.getFileName());
            fileStatus.setAuditStatus(FAILURE);
            registerFileStatusBean(applicationContext, routeProperties.getFileName(), fileStatus, camelContext);

            globalOptions.put(IS_EXCEPTION_HANDLED, TRUE.toString());
            globalOptions.put(ERROR_MESSAGE, exception.getMessage());
            globalOptions.put(FILE_NAME, routeProperties.getFileName());
            globalOptions.put(TABLE_NAME, routeProperties.getTableName());
            auditService.auditException(camelContext, exception.getMessage());
            fileResponseProcessor.process(exchange);
        }
    }
}
