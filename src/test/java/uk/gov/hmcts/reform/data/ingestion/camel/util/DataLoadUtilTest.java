package uk.gov.hmcts.reform.data.ingestion.camel.util;

import java.sql.Timestamp;

import org.apache.camel.CamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.data.ingestion.camel.helper.JrdTestSupport;

@RunWith(CamelSpringRunner.class)
@Configuration()
@ContextConfiguration(classes = DataLoadUtil.class)
public class DataLoadUtilTest extends CamelTestSupport {

    @Autowired
    DataLoadUtil dataLoadUtil;

    @Test
    public void setGlobalConstant() throws Exception {
        CamelContext camelContext = createCamelContext();
        camelContext.start();
        dataLoadUtil.setGlobalConstant(camelContext, "judicial_leaf_scheduler");
        assertNotNull("judicial_leaf_scheduler",camelContext.getGlobalOption(MappingConstants.SCHEDULER_NAME));
    }

    @Test
    public void test_getDateTimeStamp() {
        Timestamp ts = DataLoadUtil.getDateTimeStamp(JrdTestSupport.createCurrentLocalDate());
        assertNotNull(ts);
    }

    @Test
    public void test_getCurrentTimeStamp() {
        Timestamp ts = DataLoadUtil.getCurrentTimeStamp();
        assertNotNull(ts);
    }
}