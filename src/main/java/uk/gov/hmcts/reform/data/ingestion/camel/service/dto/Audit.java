package uk.gov.hmcts.reform.data.ingestion.camel.service.dto;

import lombok.Data;
import java.util.Date;

@Data
public class Audit {
    private String fileName;
    private Date schedulerStartTime;
    private String status;
}
