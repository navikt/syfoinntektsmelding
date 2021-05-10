package no.nav.syfo.config

import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil


class VaultHikariConfig (private var enabled: Boolean,
                         private var databaseBackend: String?,
                         private var databaseRole: String?,
                         private var databaseAdminrole: String?){

 /*   fun dataSource(properties: DataSourceProperties): HikariDataSource {
        return buildDatasource(properties)
    }

    @SneakyThrows
    fun buildDatasource(properties: DataSourceProperties): HikariDataSource {
        val config: HikariConfig = createHikariConfig(properties)
          val config = HikariConfig()
            config.setJdbcUrl(properties.getUrl())
            config.setMinimumIdle(1)
            config.setMaximumPoolSize(2)
        if (enabled) {
            return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                config,
                databaseBackend,
                databaseAdminrole
            )
        }
        config.setUsername(properties.getUsername())
        config.setPassword(properties.getPassword())
        return HikariDataSource(config)
    }

    @Bean
    fun flywayMigrationStrategy(properties: DataSourceProperties): FlywayMigrationStrategy {
        return if (enabled) {
            FlywayMigrationStrategy { flyway ->
                Flyway.configure()
                    .dataSource(buildDatasource(properties))
                    .initSql(String.format("SET ROLE \"%s\"", databaseAdminrole))
                    .load()
                    .migrate()
            }
        } else FlywayMigrationStrategy { flyway ->
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
            config.setJdbcUrl(properties.getUrl())
            config.setMinimumIdle(1)
            config.setMaximumPoolSize(2)
            return config
        }
    }*/
}
