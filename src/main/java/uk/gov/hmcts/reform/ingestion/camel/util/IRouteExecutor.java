package uk.gov.hmcts.reform.ingestion.camel.util;

import org.apache.camel.CamelContext;

public interface IRouteExecutor {

    void execute(CamelContext camelContext, String schedulerName, String route);
}
