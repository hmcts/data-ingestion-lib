package uk.gov.hmcts.reform.data.ingestion.camel.util;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.FileStatus;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
        globalOptions.put(MappingConstants.SCHEDULER_NAME, schedulerName);
    }

    public static boolean isFileExecuted(CamelContext camelContext, String file) {
        return nonNull(camelContext.getRegistry().lookupByName(file)) ? true : false;
    }

    public static FileStatus getFileDetails(CamelContext camelContext, String file) {
        FileStatus fileStatus = (FileStatus) camelContext.getRegistry().lookupByName(file);

        return nonNull(fileStatus) ? fileStatus : FileStatus.builder().fileName(file).build();
    }

    public static void registerFileStatusBean(ApplicationContext applicationContext, String fileName,
                                              FileStatus fileStatus, CamelContext camelContext) {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)
            applicationContext).getBeanFactory();
        if (isNull(camelContext.getRegistry().lookupByName(fileName))) {
            beanFactory.registerSingleton(fileName, fileStatus);
        }
    }
}
