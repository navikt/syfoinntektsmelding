package no.nav.syfo.localconfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TestApplicationConfig {

    public TestApplicationConfig(Environment environment) {
        System.setProperty("SECURITYTOKENSERVICE_URL", environment.getProperty("securitytokenservice.url"));
        System.setProperty("SRVSYFOMOTTAK_USERNAME", environment.getProperty("srvsyfomottak.username"));
        System.setProperty("SRVSYFOMOTTAK_PASSWORD", environment.getProperty("srvsyfomottak.password"));
    }
}
