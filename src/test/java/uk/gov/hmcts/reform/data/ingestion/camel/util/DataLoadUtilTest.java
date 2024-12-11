package uk.gov.hmcts.reform.data.ingestion.camel.util;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.data.ingestion.camel.helper.JrdTestSupport;

import java.sql.Timestamp;

import static org.junit.Assert.assertNotNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {DataLoadUtil.class, CamelAutoConfiguration.class})
@CamelSpringTest
public class DataLoadUtilTest {

    @Autowired
    DataLoadUtil dataLoadUtil;

    @Autowired
    private CamelContext camelContext;

    @Test
    public void setGlobalConstant() throws Exception {
        dataLoadUtil.setGlobalConstant(camelContext, "judicial_leaf_scheduler");
        assertNotNull("judicial_leaf_scheduler", camelContext.getGlobalOption(MappingConstants.SCHEDULER_NAME));
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