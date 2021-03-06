package uk.gov.hmcts.reform.data.ingestion.camel.util;

import org.apache.camel.CamelContext;

public interface IRouteExecutor {

    String execute(CamelContext camelContext, String schedulerName, String route);
}
