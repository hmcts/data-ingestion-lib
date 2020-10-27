package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.FileStatus;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;

import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_STATUS;

/**
 * This FileResponseProcessor stores
 * File status and state in camel registry for Auditing and archiving
 * with look up in camel registry.
 *
 * @since 2020-10-27
 */
@Component
public class FileResponseProcessor implements Processor {

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public void process(Exchange exchange) {
        final String status = exchange.getContext().getGlobalOption(SCHEDULER_STATUS);
        RouteProperties routeProperties = (RouteProperties) exchange.getIn().getHeader(ROUTE_DETAILS);
        String fileName = routeProperties.getFileName();
        FileStatus fileStatus = FileStatus.builder()
            .executionStatus(status)
            .fileName(fileName).build();
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)
            applicationContext).getBeanFactory();
        beanFactory.registerSingleton(fileName, fileStatus);
    }
}
