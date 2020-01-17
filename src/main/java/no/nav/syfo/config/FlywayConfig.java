package no.nav.syfo.config;

import com.bettercloud.vault.response.LogicalResponse;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.vault.jdbc.hikaricp.VaultUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "vault.enabled", matchIfMissing = true)
//@ConditionalOnProperty(value="spring.cloud.vault.database.enabled")
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer(VaultHikariConfig.VaultConfig vaultConfig, DataSourceProperties properties) {
        return configuration -> {
            String adminRole = vaultConfig.getDatabaseAdminrole();
            String path = vaultConfig.getDatabaseBackend() + "/creds/" + adminRole;
            log.info("Getting credentials for role {}", adminRole);
            LogicalResponse response = read(path);
            String username = response.getData().get("username");
            String password = response.getData().get("password");
            log.info("Setting datasource for flyway with user {} and role {}", username, adminRole);

            HikariConfig config = VaultHikariConfig.createHikariConfig(properties);
            config.setUsername(username);
            config.setPassword(password);

            configuration
                .dataSource(new HikariDataSource(config))
                .initSql(String.format("SET ROLE \"%s\"", adminRole));
        };
    }

    @SneakyThrows
    private LogicalResponse read(String path) {
        return VaultUtil.getInstance().getClient().logical().read(path);
    }
}
