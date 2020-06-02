package uk.gov.hmcts.reform.ingestion.camel.util;

import static uk.gov.hmcts.reform.ingestion.camel.util.MappingConstants.SCHEDULER_NAME;
import static uk.gov.hmcts.reform.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.springframework.stereotype.Component;

@Component
public class DataLoadUtil {

    public static Timestamp getCurrentTimeStamp() {

        return new Timestamp(new Date().getTime());
    }

    public static Timestamp getDateTimeStamp(String date) {
        return Timestamp.valueOf(date);
    }

    public void setGlobalConstant(CamelContext camelContext, String schedulerName) {
        Map<String, String> globalOptions = camelContext.getGlobalOptions();
        globalOptions.put(SCHEDULER_START_TIME, String.valueOf(new Date().getTime()));
        globalOptions.put(SCHEDULER_NAME, schedulerName);
    }
}
