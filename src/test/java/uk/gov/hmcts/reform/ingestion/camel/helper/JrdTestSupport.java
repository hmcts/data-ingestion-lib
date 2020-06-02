package uk.gov.hmcts.reform.ingestion.camel.helper;

import static uk.gov.hmcts.reform.ingestion.camel.util.MappingConstants.DATE_FORMAT;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import uk.gov.hmcts.reform.ingestion.camel.route.beans.RouteProperties;

public class JrdTestSupport {

    private JrdTestSupport() {

    }



    public static RouteProperties createRoutePropertiesMock() {

        RouteProperties routeProperties = new RouteProperties();
        routeProperties.setBinder("Binder");
        routeProperties.setBlobPath("Blobpath");
        routeProperties.setChildNames("childNames");
        routeProperties.setMapper("mapper");
        routeProperties.setProcessor("processor");
        routeProperties.setRouteName("routeName");
        routeProperties.setSql("sql");
        routeProperties.setTruncateSql("truncateSql");
        return routeProperties;
    }

    public static String createCurrentLocalDate() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = getDateFormatter();
        return date.format(formatter);
    }

    public static DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern(DATE_FORMAT);
    }


    public static String getDateWithFormat(Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static String getDateTimeWithFormat(LocalDateTime dateTime) {
        String datTime = dateTime.toString().replace("T", " ");
        String tail = datTime.substring(datTime.lastIndexOf(".")).concat("000000");
        return datTime.substring(0, datTime.lastIndexOf(".")) + tail;
    }
}
