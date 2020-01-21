package no.nav.syfo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@EnableJpaRepositories("no.nav.syfo.repository")
@Configuration
public class VaultHikariConfig {

    @Value("${vault.enabled:true}")
    public boolean enabled = true;
    @Value("${vault.backend}")
    public String databaseBackend;
    @Value("${vault.role:syfoinntektsmelding-user}")
    public String databaseRole;
    @Value("${vault.admin:syfoinntektsmelding-admin}")
    public String databaseAdminrole;

    @Bean
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return buildDatasource(properties);
    }

    static HikariConfig createHikariConfig(DataSourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(2);
        return config;
    }

    @SneakyThrows
    public HikariDataSource buildDatasource(DataSourceProperties properties){
        HikariConfig config = createHikariConfig(properties);
        if (enabled) {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, databaseBackend, databaseRole);
        }
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        return new HikariDataSource(config);
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSourceProperties properties) {
        return flyway -> Flyway.configure()
            .dataSource(buildDatasource(properties))
            .initSql(String.format("SET ROLE \"%s\"", databaseAdminrole))
            .load()
            .migrate();
    }

}
