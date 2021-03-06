package uk.gov.hmcts.reform.data.ingestion.camel.route.beans;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.data.ingestion.camel.helper.JrdTestSupport;

import static org.junit.Assert.assertEquals;

public class RoutePropertiesTest {

    @Test
    public void test_objects_RouteProperties_correctly() {

        RouteProperties routeProperties = JrdTestSupport.createRoutePropertiesMock();

        assertEquals("Binder", routeProperties.getBinder());
        assertEquals("Blobpath", routeProperties.getBlobPath());
        assertEquals("childNames", routeProperties.getChildNames());
        assertEquals("mapper", routeProperties.getMapper());
        assertEquals("processor", routeProperties.getProcessor());
        assertEquals("routeName", routeProperties.getRouteName());
        assertEquals("sql", routeProperties.getSql());
        assertEquals("truncateSql", routeProperties.getTruncateSql());
    }
}
