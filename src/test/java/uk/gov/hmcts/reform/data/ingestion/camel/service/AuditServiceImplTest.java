package uk.gov.hmcts.reform.data.ingestion.camel.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_NAME;
import static uk.gov.hmcts.reform.data.ingestion.camel.util.MappingConstants.SCHEDULER_START_TIME;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuditServiceImplTest {

    JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
    AuditServiceImpl dataLoadAuditUnderTest = new AuditServiceImpl();
    PlatformTransactionManager platformTransactionManager = mock(PlatformTransactionManager.class);
    TransactionStatus transactionStatus = mock(TransactionStatus.class);

    Exchange exchange = mock(Exchange.class);
    CamelContext camelContext = mock(CamelContext.class);

    final String schedulerName = "judicial_main_scheduler";



    public static Map<String, String> getGlobalOptions(String schedulerName) {
        Map<String, String> globalOptions = new HashMap<>();
        globalOptions.put("ORCHESTRATED_ROUTE", "JUDICIAL_REF_DATA_ORCHESTRATION");
        globalOptions.put(SCHEDULER_START_TIME, String.valueOf(new Date().getTime()));
        globalOptions.put(SCHEDULER_NAME, schedulerName);
        return globalOptions;
    }

    @Before
    public void setUp() {
        setField(dataLoadAuditUnderTest, "jdbcTemplate", mockJdbcTemplate);
        setField(dataLoadAuditUnderTest, "platformTransactionManager", platformTransactionManager);
        setField(dataLoadAuditUnderTest,"invalidExceptionSql", "select * from appointment");
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSchedulerAuditUpdate() throws Exception {

        Map<String, String> globalOptions = getGlobalOptions(schedulerName);
        when(exchange.getContext()).thenReturn(camelContext);
        when(exchange.getContext().getGlobalOptions()).thenReturn(globalOptions);
        when(mockJdbcTemplate.update(anyString(), anyString(), any(), any(), any())).thenReturn(1);
        when(platformTransactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(platformTransactionManager).commit(transactionStatus);
        dataLoadAuditUnderTest.auditSchedulerStatus(camelContext);
        verify(exchange, times(1)).getContext();
        verify(camelContext, times(1)).getGlobalOptions();
        verify(mockJdbcTemplate, times(1)).update(any(), (Object[]) any());
        verify(platformTransactionManager, times(1)).getTransaction(any());
        verify(platformTransactionManager, times(1)).commit(transactionStatus);
    }

    @Test
    public void testAuditException()  {
        Map<String, String> globalOptions = getGlobalOptions(schedulerName);
        when(exchange.getContext()).thenReturn(camelContext);
        when(exchange.getContext().getGlobalOptions()).thenReturn(globalOptions);
        when(mockJdbcTemplate.update(any(), any(Object[].class))).thenReturn(1);
        when(platformTransactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(platformTransactionManager).commit(transactionStatus);
        dataLoadAuditUnderTest.auditException(camelContext, "exceptionMessage");
        verify(exchange, times(1)).getContext();
        verify(mockJdbcTemplate, times(1)).update(any(), (Object[]) any());
        verify(platformTransactionManager, times(1)).getTransaction(any());
        verify(platformTransactionManager, times(1)).commit(transactionStatus);
    }
}
