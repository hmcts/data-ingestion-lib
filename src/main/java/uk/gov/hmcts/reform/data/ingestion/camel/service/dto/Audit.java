package uk.gov.hmcts.reform.data.ingestion.camel.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Audit {
    private String fileName;
    private Date schedulerStartTime;
    private String status;
}
