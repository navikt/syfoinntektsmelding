package no.nav.syfo.util

import io.ktor.server.config.ApplicationConfig

enum class AppEnv {
    LOCAL,
    DEV,
    PROD
}

fun ApplicationConfig.getEnvironment(): AppEnv = AppEnv.valueOf(getString("koin.profile"))

fun ApplicationConfig.getString(path: String): String = property(path).getString()
