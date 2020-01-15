package no.nav.syfo.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class FlywayConfig {

    @Bean
    Flyway flyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).cleanDisabled(true).schemas().load();
    }

    @Bean
    public FlywayConfigurationCustomizer flywayConfig( @Value("${vault.postgres.role}") String role) {
        return c -> c
            .initSql(String.format("SET ROLE \"%s\"", role));
    }

    @Bean
    FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, null);
    }
}
