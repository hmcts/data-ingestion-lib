package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.FileStatus;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.validator.JsrValidatorInitializer;

import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.DataLoadUtil.getFileDetails;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.DataLoadUtil.registerFileStatusBean;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.PARTIAL_SUCCESS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;

/**
 * This JsrValidationBaseProcessor captures JSR violations related to blob files.
 * and log those in exception table.
 *
 * @since 2020-10-27
 */
@Slf4j
public abstract class JsrValidationBaseProcessor<T> implements Processor {

    @Value("${jsr-threshold-limit:0}")
    int jsrThresholdLimit;

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    private List<T> invalidRecords;

    @Autowired
    ApplicationContext applicationContext;

    public List<T> validate(JsrValidatorInitializer<T> jsrValidatorInitializer, List<T> list) {
        List<T> validRecords = jsrValidatorInitializer.validate(list);
        invalidRecords = jsrValidatorInitializer.getInvalidJsrRecords();
        return jsrValidatorInitializer.validate(list);
    }

    /**
     * Audit JSR exceptions in file.
     *
     * @param jsrValidatorInitializer JsrValidatorInitializer
     * @param exchange                Exchange
     */
    public void audit(JsrValidatorInitializer<T> jsrValidatorInitializer, Exchange exchange) {
        RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
        FileStatus fileStatus = getFileDetails(exchange.getContext(), routeProperties.getFileName());

        if (nonNull(jsrValidatorInitializer.getConstraintViolations())
            && jsrValidatorInitializer.getConstraintViolations().size() > 0) {
            log.warn("{}:: Jsr exception in {} {} ", logComponentName, getClass().getSimpleName(),
                "Please check database table");
            //Auditing JSR exceptions in exception table
            jsrValidatorInitializer.auditJsrExceptions(exchange);
            fileStatus.setAuditStatus(PARTIAL_SUCCESS);
            registerFileStatusBean(applicationContext, routeProperties.getFileName(), fileStatus,
                exchange.getContext());
        }

        //jsrThresholdLimit=0 we are not failing for any threshold limit
        if (FALSE.equals(jsrThresholdLimit == 0)
            && jsrValidatorInitializer.getConstraintViolations().size() > jsrThresholdLimit) {
            fileStatus.setAuditStatus(PARTIAL_SUCCESS);
            registerFileStatusBean(applicationContext, routeProperties.getFileName(), fileStatus,
                exchange.getContext());
            throw new RouteFailedException("Jsr exception exceeds threshold limit in "
                + this.getClass().getSimpleName());
        }
    }

    public List<T> getInvalidRecords() {
        return invalidRecords;
    }
}
