package no.nav.syfo.localconfig;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
public class TestApplicationConfig {

    public TestApplicationConfig(Environment environment) {
        System.setProperty("SECURITYTOKENSERVICE_URL", environment.getProperty("securitytokenservice.url"));
        System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", environment.getProperty("srvsyfoinntektsmelding.username"));
        System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", environment.getProperty("srvsyfoinntektsmelding.password"));
    }

    // Sørger for at flyway migrering skjer etter at JTA transaction manager er ferdig satt opp av Spring.
    // Forhindrer WARNING: transaction manager not running? loggspam fra Atomikos.
    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy(final JtaTransactionManager jtaTransactionManager) {
        return Flyway::migrate;
    }
}
