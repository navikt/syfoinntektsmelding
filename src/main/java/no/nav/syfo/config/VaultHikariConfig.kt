package no.nav.syfo.config

import lombok.extern.slf4j.Slf4j
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import com.zaxxer.hikari.HikariDataSource
import lombok.SneakyThrows
import com.zaxxer.hikari.HikariConfig
import no.nav.syfo.config.VaultHikariConfig
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Slf4j
@EnableJpaRepositories("no.nav.syfo.repository")
@Configuration
class VaultHikariConfig {
    @Value("\${vault.enabled:true}")
    var enabled = true

    @Value("\${vault.backend}")
    var databaseBackend: String? = null

    @Value("\${vault.role:syfoinntektsmelding-user}")
    var databaseRole: String? = null

    @Value("\${vault.admin:syfoinntektsmelding-admin}")
    var databaseAdminrole: String? = null
    @Bean
    fun dataSource(properties: DataSourceProperties): HikariDataSource {
        return buildDatasource(properties)
    }

    @SneakyThrows
    fun buildDatasource(properties: DataSourceProperties): HikariDataSource {
        val config = createHikariConfig(properties)
        if (enabled) {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                config,
                databaseBackend,
                databaseAdminrole
            )
        }
        config.username = properties.username
        config.password = properties.password
        return HikariDataSource(config)
    }

    @Bean
    fun flywayMigrationStrategy(properties: DataSourceProperties): FlywayMigrationStrategy {
        return if (enabled) {
            FlywayMigrationStrategy {
                Flyway.configure()
                    .dataSource(buildDatasource(properties))
                    .initSql(String.format("SET ROLE \"%s\"", databaseAdminrole))
                    .load()
                    .migrate()
            }
        } else FlywayMigrationStrategy {
            Flyway.configure()
                .dataSource(buildDatasource(properties))
                .load()
                .migrate()
        }
        // Ikke kjør rolle settingen i testing. De feiler
    }

    companion object {
        fun createHikariConfig(properties: DataSourceProperties): HikariConfig {
            val config = HikariConfig()
            config.jdbcUrl = properties.url
            config.minimumIdle = 1
            config.maximumPoolSize = 2
            return config
        }
    }
}
