package no.nav.syfo.localconfig;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

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
    FlywayMigrationStrategy flywayMigrationStrategy(final DataSourceTransactionManager datasourceTransactionManager) {
        return Flyway::migrate;
    }

    @Bean
    public DataSourceTransactionManager datasourceTransactionManager(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);
        return dataSourceTransactionManager;
    }
}
