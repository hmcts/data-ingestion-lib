package uk.gov.hmcts.reform.data.ingestion.camel.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Long.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FILENOTPRESENT_ERRORMESSAGE;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_NAME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_STATUS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SUCCESS;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import java.sql.Timestamp;
import java.util.Map;

@Slf4j
@Component
public class AuditServiceImpl implements IAuditService {

    @Autowired
    @Qualifier("springJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Value("${scheduler-insert-sql}")
    protected String schedulerInsertSql;

    @Autowired
    @Qualifier("springJdbcTransactionManager")
    protected PlatformTransactionManager platformTransactionManager;

    @Value("${scheduler-audit-select}")
    protected String getSchedulerAuditDetails;

    @Value("${invalid-exception-sql}")
    String invalidExceptionSql;

    /**
     * Updates scheduler details.
     *
     * @param camelContext CamelContext
     * @return void
     */
    public void auditSchedulerStatus(final CamelContext camelContext) {

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("Auditing scheduler details");

        Map<String, String> globalOptions = camelContext.getGlobalOptions();

        Timestamp schedulerStartTime = new Timestamp(valueOf((globalOptions.get(SCHEDULER_START_TIME))));
        String schedulerName = globalOptions.get(SCHEDULER_NAME);
        String schedulerStatus = isNull(globalOptions.get(SCHEDULER_STATUS)) ? SUCCESS
            : globalOptions.get(SCHEDULER_STATUS);

        jdbcTemplate.update(schedulerInsertSql, schedulerName, schedulerStartTime, new Timestamp(currentTimeMillis()),
            schedulerStatus);
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        platformTransactionManager.commit(status);
    }

    /**
     * check auditing is done/not.
     *
     * @return boolean
     */
    public boolean isAuditingCompleted() {
        return jdbcTemplate.queryForObject(getSchedulerAuditDetails, Integer.class) > 1 ? TRUE : FALSE;
    }

    /**
     * Prepare for persisting exception by constructing query and parameters.
     *
     * @param camelContext of camel routes
     * @param exceptionMessage for exception record
     * @param auditExceptionForFileTimeStamp flag to decide auditing file record timestamp
     */
    @Override
    public void auditException(final CamelContext camelContext, String exceptionMessage,
                               boolean auditExceptionForFileTimeStamp) {
        Map<String, String> globalOptions = camelContext.getGlobalOptions();
        Timestamp schedulerStartTime = new Timestamp(Long.valueOf((globalOptions.get(SCHEDULER_START_TIME))));
        Timestamp updatedTimeStamp = new Timestamp(currentTimeMillis());
        String schedulerName = globalOptions.get(SCHEDULER_NAME);
        String fileTimeStampError = StringUtils.EMPTY;
        if (auditExceptionForFileTimeStamp) {
            fileTimeStampError = FILENOTPRESENT_ERRORMESSAGE;
        }
        persistException(invalidExceptionSql, new Object[]{
            schedulerStartTime, schedulerName, fileTimeStampError, exceptionMessage, updatedTimeStamp});
    }

    /**
     * Persists exception record in database with given query and params.
     *
     * @param query for inserting record into db
     * @param params for constructing query
     */

    private void persistException(String query, Object[] params) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        jdbcTemplate.update(query, params);
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        platformTransactionManager.commit(status);
    }

}
