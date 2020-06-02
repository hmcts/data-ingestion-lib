package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import static java.util.Objects.nonNull;

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

    @Value("${jsr-threshold-limit}")
    int jsrThresholdLimit;

    public List<T> validate(JsrValidatorInitializer<T> jsrValidatorInitializer, List<T> list) {
        return jsrValidatorInitializer.validate(list);
    }

    public void audit(JsrValidatorInitializer<T> jsrValidatorInitializer, Exchange exchange) {

        if (nonNull(jsrValidatorInitializer.getConstraintViolations())
                && jsrValidatorInitializer.getConstraintViolations().size() > 0) {
            log.warn("Jsr exception in" + this.getClass().getSimpleName() + "Please check database table");
            //Auditing JSR exceptions in exception table
            jsrValidatorInitializer.auditJsrExceptions(exchange);
            exchange.getContext().getGlobalOptions().put(MappingConstants.SCHEDULER_STATUS, MappingConstants.PARTIAL_SUCCESS);
        }

        if (jsrValidatorInitializer.getConstraintViolations().size() > jsrThresholdLimit) {
            exchange.getContext().getGlobalOptions().put(MappingConstants.SCHEDULER_STATUS, MappingConstants.FAILURE);
            throw new RouteFailedException("Jsr exception exceeds threshold limit in " + this.getClass().getSimpleName());
        }
    }
}
