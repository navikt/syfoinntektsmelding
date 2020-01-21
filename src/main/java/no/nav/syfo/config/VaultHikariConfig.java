package no.nav.syfo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
    public HikariDataSource dataSource(DataSourceProperties properties) throws VaultError {
        System.out.println("enabled: " + enabled);
        System.out.println("databaseBackend: " + databaseBackend);
        System.out.println("Vault: " + databaseRole);
        System.out.println("databaseRole: " + databaseRole);
        System.out.println("databaseAdminrole: " + databaseAdminrole);
        HikariConfig config = createHikariConfig(properties);
        if (enabled) {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, databaseBackend, databaseRole);
        }
        config.setUsername("tullball");
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
