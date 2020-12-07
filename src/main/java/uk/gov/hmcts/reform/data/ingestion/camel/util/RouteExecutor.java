package uk.gov.hmcts.reform.data.ingestion.camel.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.data.ingestion.camel.route.beans.FileStatus;
import uk.gov.hmcts.reform.data.ingestion.camel.service.IEmailService;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.DataLoadUtil.getFileDetails;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.EXECUTION_FAILED;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FAILURE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SUCCESS;

@Slf4j
@Component
public abstract class RouteExecutor implements IRouteExecutor {

    @Autowired
    protected CamelContext camelContext;

    @Autowired
    protected DataLoadUtil dataLoadUtil;

    @Autowired
    protected ProducerTemplate producerTemplate;

    @Autowired
    protected IEmailService emailService;

    @Value("${archival-file-names}")
    List<String> archivalFileNames;

    @Override
    public String execute(CamelContext camelContext, String schedulerName, String route) {
        try {
            Map<String, String> globalOptions = camelContext.getGlobalOptions();
            globalOptions.remove(MappingConstants.IS_EXCEPTION_HANDLED);
            globalOptions.remove(MappingConstants.SCHEDULER_STATUS);
            dataLoadUtil.setGlobalConstant(camelContext, schedulerName);
            producerTemplate.sendBody(route, "starting " + schedulerName);
            return SUCCESS;
        } finally {
            List<FileStatus> fileStatuses = archivalFileNames.stream().map(s -> getFileDetails(camelContext, s))
                .filter(fileStatus -> nonNull(fileStatus.getAuditStatus())
                    && fileStatus.getAuditStatus().equalsIgnoreCase(FAILURE))
                .collect(toList());
            if (isNotTrue(CollectionUtils.isEmpty(fileStatuses))) {
                emailService.sendEmail(EXECUTION_FAILED,
                    join(fileStatuses, ","));
            }
        }
    }
}
