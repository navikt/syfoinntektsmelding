package no.nav.syfo.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory

fun createHikariConfig(
    jdbcUrl: String,
    username: String? = null,
    password: String? = null,
    prometheusMetricsTrackerFactory: PrometheusMetricsTrackerFactory? = null,
) = HikariConfig().apply {
    this.jdbcUrl = jdbcUrl
    maximumPoolSize = 3
    minimumIdle = 1
    idleTimeout = 10001
    connectionTimeout = 2000
    maxLifetime = 30001
    driverClassName = "org.postgresql.Driver"
    username?.let { this.username = it }
    password?.let { this.password = it }
    poolName = "defaultPool"
    prometheusMetricsTrackerFactory?.let { metricsTrackerFactory = prometheusMetricsTrackerFactory }
}

fun createTestHikariConfig() = createHikariConfig("jdbc:postgresql://localhost:5432/spinn", "spinn", "spinn", null)
