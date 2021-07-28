package uk.gov.hmcts.reform.data.ingestion.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.FileStatus;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.util.DataLoadUtil;

import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FAILURE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_PARENT_FAILED;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.ROUTE_DETAILS;

@Component
public class ParentStateCheckProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {

        RouteProperties routeProperties = (RouteProperties) exchange.getIn()
            .getHeader(ROUTE_DETAILS);
        if (routeProperties.isParentFailureEnabled()) {
            String parentFileName = routeProperties.getParentFileName();
            FileStatus fileStatus = DataLoadUtil.getFileDetails(exchange.getContext(), parentFileName);
            if (FAILURE.equalsIgnoreCase(fileStatus.getAuditStatus())) {
                exchange.getMessage().setHeader(IS_PARENT_FAILED, true);
                return;
            }
        }
        exchange.getMessage().setHeader(IS_PARENT_FAILED, false);
    }
}
