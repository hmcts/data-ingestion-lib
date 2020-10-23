package uk.gov.hmcts.reform.data.ingestion.camel.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.data.ingestion.camel.route.ArchivalRoute;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.DataLoadUtil.getFileName;

@Component
@Slf4j
public class ArchivalBlobServiceImpl implements IArchivalBlobService {

    @Value("${archival-file-names}")
    List<String> archivalFileNames;

    @Autowired
    ArchivalRoute archivalRoute;

    @Autowired
    ProducerTemplate producerTemplate;

    @Value("${archival-route}")
    String archivalRouteName;

    @Autowired
    CamelContext camelContext;

    @Override
    public void executeArchiving() {

        List<String> nonStaleFiles = archivalFileNames.stream().filter(file ->
            getFileName(camelContext, file) != null && getFileName(camelContext, file).equals(file))
            .collect(toList());

        archivalRoute.archivalRoute(nonStaleFiles);
        producerTemplate.sendBody(archivalRouteName, "starting Archival");
    }
}
