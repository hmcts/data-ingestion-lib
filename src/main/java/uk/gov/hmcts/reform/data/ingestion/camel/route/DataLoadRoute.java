package uk.gov.hmcts.reform.data.ingestion.camel.route;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.FailedToCreateRouteException;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.data.ingestion.camel.processor.ExceptionProcessor;
import uk.gov.hmcts.reform.data.ingestion.camel.processor.FileReadProcessor;
import uk.gov.hmcts.reform.data.ingestion.camel.processor.FileResponseProcessor;
import uk.gov.hmcts.reform.data.ingestion.camel.processor.HeaderValidationProcessor;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.RouteProperties;
import uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Resource;
import javax.sql.DataSource;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang.WordUtils.uncapitalize;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.DIRECT_ROUTE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.IS_FILE_STALE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.MAPPING_METHOD;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.TRUNCATE_ROUTE_PREFIX;

/**
 * This DataLoadRoute is camel DSL route to execute and process blob files
 * Process blob file includes
 * validation,transformation and storing the data in database with camel datasource.
 *
 * @since 2020-10-27
 */
@Component
public class DataLoadRoute {

    @Autowired
    FileReadProcessor fileReadProcessor;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    Environment environment;

    @Resource(name = "PROPAGATION_REQUIRES_NEW")
    SpringTransactionPolicy springTransactionPolicyNew;

    @Resource(name = "PROPAGATION_REQUIRED")
    SpringTransactionPolicy springTransactionPolicy;

    @Autowired
    ExceptionProcessor exceptionProcessor;

    @Autowired
    CamelContext camelContext;

    @Autowired
    HeaderValidationProcessor headerValidationProcessor;

    @Autowired
    FileResponseProcessor fileResponseProcessor;

    @Autowired
    DataSource dataSource;

    @Transactional("txManager")
    public void startRoute(String startRoute, List<String> routesToExecute) throws FailedToCreateRouteException {

        List<RouteProperties> routePropertiesList = getRouteProperties(routesToExecute);


        try {
            camelContext.addRoutes(
                new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {

                        onException(Exception.class)
                            .handled(true)
                            .process(exceptionProcessor)
                            .end()
                            .markRollbackOnly()
                            .end();


                        String[] multiCastRoute = createDirectRoutesForMulticast(routesToExecute);

                        //Started direct route with multi-cast all the configured routes with
                        //Transaction propagation required eg.application-jrd-router.yaml(rd-judicial-data-load)
                        from(startRoute)
                            .multicast()
                            .to(multiCastRoute).end();

                        for (RouteProperties route : routePropertiesList) {

                            Expression exp = new SimpleExpression(route.getBlobPath());
                            List<String> sqls = new ArrayList<>();
                            int loopCount = getLoopCount(route, sqls);

                            from(DIRECT_ROUTE + route.getRouteName()).id(DIRECT_ROUTE + route.getRouteName())
                                .transacted().policy(springTransactionPolicy)
                                .process(headerValidationProcessor)
                                .split(body()).unmarshal().bindy(BindyType.Csv,
                                applicationContext.getBean(route.getBinder()).getClass())
                                .process((Processor) applicationContext.getBean(route.getProcessor()))
                                .loop(loopCount)
                                //delete & Insert process
                                .split().body()
                                .streaming()
                                .bean(applicationContext.getBean(route.getMapper()), MAPPING_METHOD)

                                .process(exchange -> {
                                    Integer index = (Integer) exchange
                                        .getProperty(Exchange.LOOP_INDEX);
                                    exchange.getIn().setHeader("sqlToExecute", sqls.get(index));
                                })
                                .toD("${header.sqlToExecute}")
                                .end()
                                .process(fileResponseProcessor)
                                .end()
                                .end(); //end route

                            //Route reads file, truncates and then call main file route via below line
                            //to(DIRECT_ROUTE + route.getRouteName())
                            //with Spring Propagation new for each file
                            from(DIRECT_ROUTE + TRUNCATE_ROUTE_PREFIX + route.getRouteName())
                                .transacted()
                                .policy(springTransactionPolicyNew)
                                .setHeader(MappingConstants.ROUTE_DETAILS, () -> route)
                                .setProperty(MappingConstants.BLOBPATH, exp)
                                .process(fileReadProcessor)
                                .choice()
                                .when(header(IS_FILE_STALE).isEqualTo(false))
                                .to(route.getTruncateSql())
                                .to(DIRECT_ROUTE + route.getRouteName())
                                .endChoice()
                                .end();
                        }
                    }
                });
        } catch (Exception ex) {
            throw new FailedToCreateRouteException(" Data Load - failed to start for route ", startRoute,
                startRoute, ex);
        }
    }


    private String[] createDirectRoutesForMulticast(List<String> routeList) {
        int index = 0;
        String[] directRouteNameList = new String[routeList.size()];
        for (String child : routeList) {
            directRouteNameList[index] = (DIRECT_ROUTE).concat(TRUNCATE_ROUTE_PREFIX).concat(child);
            index++;
        }
        return directRouteNameList;
    }

    /**
     * Sets Route Properties.
     *
     * @param routes routes
     * @return List RouteProperties.
     */
    private List<RouteProperties> getRouteProperties(List<String> routes) {
        List<RouteProperties> routePropertiesList = new LinkedList<>();
        int index = 0;
        for (String routeName : routes) {
            RouteProperties properties = new RouteProperties();
            properties.setRouteName(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.ID));
            properties.setSql(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.INSERT_SQL));
            properties.setTruncateSql(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.TRUNCATE_SQL)
                == null ? "log:test" : environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.TRUNCATE_SQL));
            properties.setBlobPath(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.BLOBPATH));
            properties.setMapper(uncapitalize(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.MAPPER)));
            properties.setBinder(uncapitalize(environment.getProperty(MappingConstants.ROUTE + "." + routeName + "."
                + MappingConstants.CSVBINDER)));
            properties.setProcessor(uncapitalize(environment.getProperty(MappingConstants.ROUTE + "." + routeName + "."
                + MappingConstants.PROCESSOR)));
            properties.setFileName(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.FILE_NAME));
            properties.setTableName(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.TABLE_NAME));
            properties.setCsvHeadersExpected(environment.getProperty(
                MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.CSV_HEADERS_EXPECTED));
            String isHeaderValidationEnabled = environment.getProperty(
                    MappingConstants.ROUTE + "." + routeName + "." + MappingConstants.IS_HEADER_VALIDATION_ENABLED);
            if (isBlank(isHeaderValidationEnabled)) {
                isHeaderValidationEnabled = Boolean.FALSE.toString();
            }
            properties.setIsHeaderValidationEnabled(isHeaderValidationEnabled);
            routePropertiesList.add(index, properties);
            properties.setDeleteSql(environment.getProperty(MappingConstants.ROUTE + "."
                + routeName + "." + MappingConstants.DELETE_SQL));
            index++;
        }
        return routePropertiesList;
    }

    private int getLoopCount(RouteProperties route, List<String> sqls) {
        if ((nonNull(route.getDeleteSql()))) {
            sqls.add(route.getDeleteSql());
        }
        sqls.add(route.getSql());
        return sqls.size();
    }
}
