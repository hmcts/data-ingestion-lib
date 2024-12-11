package uk.gov.hmcts.reform.data.ingestion.configuration;

import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStorageCredentials {

    @Autowired
    AzureBlobConfig azureBlobConfig;

    @Autowired
    @Qualifier("credsreg")
    StorageSharedKeyCredential storageCredentials;

    @Bean(name = "credsreg")
    public StorageSharedKeyCredential credentials() {
        return new StorageSharedKeyCredential(azureBlobConfig.getAccountName(), azureBlobConfig.getAccountKey());
    }

    @Bean(name = "clientBuilder")
    public BlobServiceClientBuilder clientBuilder() {
        return new BlobServiceClientBuilder();
    }

}