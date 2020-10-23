package uk.gov.hmcts.reform.data.ingestion.configuration;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageCredentials {

    @Autowired
    AzureBlobConfig azureBlobConfig;

    @Autowired
    @Qualifier("credsreg")
    com.microsoft.azure.storage.StorageCredentials storageCredentials;

    @Bean(name = "credsreg")
    public com.microsoft.azure.storage.StorageCredentials credentials() {
        return new StorageCredentialsAccountAndKey(azureBlobConfig.getAccountName(), azureBlobConfig.getAccountKey());
    }

    @Bean(name = "credscloudStorageAccount")
    public CloudStorageAccount cloudStorageAccount() throws Exception {
        return new CloudStorageAccount(storageCredentials,
            true);
    }
}