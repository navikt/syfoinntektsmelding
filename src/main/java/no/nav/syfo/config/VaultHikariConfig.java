package no.nav.syfo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import no.nav.vault.jdbc.hikaricp.VaultError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@EnableJpaRepositories("no.nav.syfo.repository")
public class VaultHikariConfig {

    @Value("${vault.enabled}")
    public boolean enabled = true;
    @Value("${vault.databaseBackend}")
    public String databaseBackend;
    @Value("${vault.databaseRole}")
    public String databaseRole;
    @Value("${vault.databaseAdminrole}")
    public String databaseAdminrole;

    @Bean
    public HikariDataSource dataSource(DataSourceProperties properties) throws VaultError {
        HikariConfig config = createHikariConfig(properties);
        if (enabled) {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, databaseBackend, databaseRole);
        }
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        return new HikariDataSource(config);
    }

    static HikariConfig createHikariConfig(DataSourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(2);
        return config;
    }

}
