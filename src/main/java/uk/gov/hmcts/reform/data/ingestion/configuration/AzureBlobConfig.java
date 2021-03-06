package uk.gov.hmcts.reform.data.ingestion.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "azure.storage")
@Setter
public class AzureBlobConfig {
    private String accountName;
    private String accountKey;
    private String containerName;
    private String blobUrlSuffix;
}
