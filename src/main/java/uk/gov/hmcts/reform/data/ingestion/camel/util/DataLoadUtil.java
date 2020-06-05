package uk.gov.hmcts.reform.data.ingestion.camel.util;

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
        globalOptions.put(MappingConstants.SCHEDULER_START_TIME, String.valueOf(new Date().getTime()));
        globalOptions.put(MappingConstants.SCHEDULER_NAME, schedulerName);
    }
}