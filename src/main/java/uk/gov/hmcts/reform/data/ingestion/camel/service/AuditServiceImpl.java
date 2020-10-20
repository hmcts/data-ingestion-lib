package uk.gov.hmcts.reform.data.ingestion.camel.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Long.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.FILE_NAME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_NAME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_STATUS;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SUCCESS;

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
     * Updates scheduler exceptions.
     *
     * @param camelContext CamelContext
     * @return void
     */
    public void auditException(final CamelContext camelContext, String exceptionMessage) {
        Map<String, String> globalOptions = camelContext.getGlobalOptions();
        Timestamp schedulerStartTime = new Timestamp(Long.valueOf((globalOptions.get(SCHEDULER_START_TIME))));
        String schedulerName = globalOptions.get(SCHEDULER_NAME);
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();

        Object[] params = new Object[]{camelContext.getGlobalOptions().get(FILE_NAME),
            schedulerStartTime, schedulerName, exceptionMessage, new Timestamp(currentTimeMillis())};
        //separate transaction manager required for auditing as it is independent form route
        //Transaction
        jdbcTemplate.update(invalidExceptionSql, params);
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

}
