package uk.gov.hmcts.reform.data.ingestion.camel.route;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.exception.RouteFailedException;
import uk.gov.hmcts.reform.data.ingestion.camel.processor.ArchiveFileProcessor;
import uk.gov.hmcts.reform.data.ingestion.camel.processor.ExceptionProcessor;

/**
 * This ArchivalRoute is camel DSL route to copy/backup blob files in archival blob.
 *
 * @since 2020-10-27
 */
@Component
public class ArchivalRoute {

    @Autowired
    CamelContext camelContext;

    @Autowired
    ExceptionProcessor exceptionProcessor;

    @Autowired
    ArchiveFileProcessor archiveFileProcessor;

    @Value("${archival-path}")
    String archivalPath;


    @Value("${archival-cred}")
    String archivalCred;

    @Value("${archival-route}")
    String archivalRoute;

    public void archivalRoute(List<String> archivalFiles) {
        try {

            camelContext.addRoutes(
                new SpringRouteBuilder() {
                    @Override
                    public void configure() throws Exception {

                        onException(Exception.class)
                            .handled(true)
                            .process(exceptionProcessor)
                            .end();

                        from(archivalRoute)
                            .loop(archivalFiles.size()).copy()
                            .process(archiveFileProcessor)
                            .toD(archivalPath + "${header.filename}?" + archivalCred)
                            .end()
                            .end();
                    }
                });
        } catch (Exception ex) {
            throw new RouteFailedException(" Data Load - failed for archival ");
        }
    }
}
