package no.nav.syfo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.ClusterAwareSpringProfileResolver;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import no.nav.vault.jdbc.hikaricp.VaultError;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@ConditionalOnProperty(value = "vault.enabled", matchIfMissing = true)
public class VaultHikariConfig {

    private static final String APPLICATION_NAME = "syfoinntektsmelding";

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Bean
    public DataSource userDataSource() {
        return dataSource("user");
    }

    private String finnMountPath() {
        String naisCluster = ClusterAwareSpringProfileResolver.profiles()[0];
        if (naisCluster == null || naisCluster.isEmpty()){
            return "postgresql/preprod-fss";
        }
        if ("dev-fss".equals(naisCluster)) {
            return "postgresql/preprod-fss";
        }
        return "postgresql/" + naisCluster;
    }

    @SneakyThrows
    private HikariDataSource dataSource(String user) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, finnMountPath(), dbRole(user));
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> Flyway.configure()
            .dataSource(dataSource("admin"))
            .initSql(String.format("SET ROLE \"%s\"", dbRole("admin")))
            .load()
            .migrate();
    }

    private String dbRole(String role) {
        return String.join("-", APPLICATION_NAME, role);
    }
}
