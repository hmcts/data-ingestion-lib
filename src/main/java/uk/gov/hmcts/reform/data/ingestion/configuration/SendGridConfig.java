package uk.gov.hmcts.reform.data.ingestion.configuration;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class SendGridConfig {

    @Value("${sendgrid.key:''}")
    String sendGridKey;

    @Bean
    @Lazy
    public SendGrid sendGrid() {
        return new SendGrid(sendGridKey);
    }
}
