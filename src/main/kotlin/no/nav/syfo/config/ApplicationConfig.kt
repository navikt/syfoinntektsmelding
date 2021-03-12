package no.nav.syfo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import javax.persistence.EntityManagerFactory
import org.springframework.orm.jpa.JpaTransactionManager
import javax.sql.DataSource

@Configuration
@EnableScheduling
@EnableTransactionManagement
@EnableJpaRepositories("no.nav.syfo.repository")
class ApplicationConfig {
    @Bean
    fun datasourceTransactionManager(dataSource: DataSource?): DataSourceTransactionManager {
        val dataSourceTransactionManager = DataSourceTransactionManager()
        dataSourceTransactionManager.dataSource = dataSource
        return dataSourceTransactionManager
    }

    @Bean(name = ["transactionManager"])
    fun getTransactionManager(emf: EntityManagerFactory?, dataSource: DataSource?): JpaTransactionManager {
        val tm = JpaTransactionManager()
        tm.entityManagerFactory = emf
        tm.dataSource = dataSource
        return tm
    }
}
