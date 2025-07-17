package no.nav.syfo.koin

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.server.config.ApplicationConfig
import no.nav.syfo.util.AppEnv.DEV
import no.nav.syfo.util.AppEnv.PROD
import no.nav.syfo.util.KubernetesProbeManager
import no.nav.syfo.util.createHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule =
        when (config.property("koin.profile").getString()) {
            PROD.name -> prodConfig(config)
            DEV.name -> devConfig(config)
            else -> localDevConfig(config)
        }
    return listOf(common, envModule)
}

val common =
    module {
        single { buildObjectMapper() }

        single { KubernetesProbeManager() }

        single {
            createHttpClient(maxRetries = 3)
        }
    }

fun buildObjectMapper(): ObjectMapper =
    ObjectMapper().apply {
        registerModule(
            KotlinModule
                .Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
        )
        registerModule(Jdk8Module())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setDefaultPrettyPrinter(
            DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            },
        )
    }

// utils

fun ApplicationConfig.getjdbcUrlFromProperties(): String =
    String.format(
        "jdbc:postgresql://%s:%s/%s",
        this.property("database.host").getString(),
        this.property("database.port").getString(),
        this.property("database.name").getString(),
    )
