package uk.gov.hmcts.reform.data.ingestion.camel.util;

import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SUCCESS;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.service.EmailService;

@Slf4j
@Component
public abstract class RouteExecutor implements IRouteExecutor {

    @Autowired
    protected CamelContext camelContext;

    @Autowired
    protected DataLoadUtil dataLoadUtil;

    @Autowired
    protected ProducerTemplate producerTemplate;

    @Autowired
    protected EmailService emailService;

    @Override
    public String execute(CamelContext camelContext, String schedulerName, String route) {
        try {
            Map<String, String> globalOptions = camelContext.getGlobalOptions();
            globalOptions.remove(MappingConstants.IS_EXCEPTION_HANDLED);
            globalOptions.remove(MappingConstants.SCHEDULER_STATUS);
            dataLoadUtil.setGlobalConstant(camelContext, schedulerName);
            producerTemplate.sendBody(route, "starting " + schedulerName);
            return SUCCESS;
        } catch (Exception ex) {
            //Camel override error stack with route failed hence grabbing exception form context
            String errorMessage = camelContext.getGlobalOptions().get(MappingConstants.ERROR_MESSAGE);
            emailService.sendEmail(errorMessage);
            throw new RouteFailedException(errorMessage);
        }
    }
}
