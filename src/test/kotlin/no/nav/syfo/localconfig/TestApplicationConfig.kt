package no.nav.syfo.localconfig

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@Profile(value = ["test"])
class TestApplicationConfig(environment: Environment) {
    // Sørger for at flyway migrering skjer etter at JTA transaction manager er ferdig satt opp av Spring.
    // Forhindrer WARNING: transaction manager not running? loggspam fra Atomikos.
    @Bean
    fun flywayMigrationStrategy(datasourceTransactionManager: DataSourceTransactionManager?): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { obj: Flyway -> obj.migrate() }
    }

    @Bean
    fun datasourceTransactionManager(@Qualifier("dataSource") dataSource: DataSource?): DataSourceTransactionManager {
        val dataSourceTransactionManager = DataSourceTransactionManager()
        dataSourceTransactionManager.dataSource = dataSource
        return dataSourceTransactionManager
    }

    init {
        System.setProperty("SECURITYTOKENSERVICE_URL", environment.getProperty("securitytokenservice.url"))
        System.setProperty(
            "SRVSYFOINNTEKTSMELDING_USERNAME",
            environment.getProperty("srvsyfoinntektsmelding.username")
        )
        System.setProperty(
            "SRVSYFOINNTEKTSMELDING_PASSWORD",
            environment.getProperty("srvsyfoinntektsmelding.password")
        )
    }
}
