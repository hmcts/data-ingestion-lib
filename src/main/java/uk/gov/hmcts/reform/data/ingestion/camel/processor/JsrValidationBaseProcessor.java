package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FAILURE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_STATUS;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants;
import uk.gov.hmcts.reform.data.ingestion.camel.validator.JsrValidatorInitializer;

@Slf4j
public abstract class JsrValidationBaseProcessor<T> implements Processor {

    @Value("${jsr-threshold-limit:0}")
    int jsrThresholdLimit;

    @Value("${logging-component-name:data_ingestion}")
    private String logComponentName;

    private List<T> invalidRecords;

    public List<T> validate(JsrValidatorInitializer<T> jsrValidatorInitializer, List<T> list) {
        List<T> validRecords = jsrValidatorInitializer.validate(list);
        invalidRecords = jsrValidatorInitializer.getInvalidJsrRecords();
        return jsrValidatorInitializer.validate(list);
    }

    public void audit(JsrValidatorInitializer<T> jsrValidatorInitializer, Exchange exchange) {

        if (nonNull(jsrValidatorInitializer.getConstraintViolations())
                && jsrValidatorInitializer.getConstraintViolations().size() > 0) {
            log.warn("{}:: Jsr exception in {} {} ", logComponentName, getClass().getSimpleName(),
                "Please check database table");
            //Auditing JSR exceptions in exception table
            jsrValidatorInitializer.auditJsrExceptions(exchange);
            exchange.getContext().getGlobalOptions().put(SCHEDULER_STATUS, MappingConstants.PARTIAL_SUCCESS);
        }

        //jsrThresholdLimit=0 we are not failing for any threshold limit
        if (FALSE.equals(jsrThresholdLimit == 0)
                && jsrValidatorInitializer.getConstraintViolations().size() > jsrThresholdLimit) {
            exchange.getContext().getGlobalOptions().put(SCHEDULER_STATUS, FAILURE);
            throw new RouteFailedException("Jsr exception exceeds threshold limit in "
                + this.getClass().getSimpleName());
        }
    }

    public List<T> getInvalidRecords() {
        return invalidRecords;
    }
}
