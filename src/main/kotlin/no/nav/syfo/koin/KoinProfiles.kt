package no.nav.syfo.koin

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module

@KtorExperimentalAPI
fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.property("koin.profile").getString()) {
        "LOCAL" -> localDevConfig(config)
        "PREPROD" -> preprodConfig(config)
        "PROD" -> prodConfig(config)
        else -> localDevConfig(config)
    }
    return listOf(common, envModule)
}

fun buildObjectMapper(): ObjectMapper {
    return ObjectMapper().apply {
        registerModule(KotlinModule())
        registerModule(KotlinModule())
        registerModule(Jdk8Module())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
            indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
            indentObjectsWith(DefaultIndenter("  ", "\n"))
        })
    }
}

fun buildJacksonSerializer(): JacksonSerializer {
    return JacksonSerializer {
        registerModule(KotlinModule())
        registerModule(Jdk8Module())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    }
}

val common = module {
    single { buildObjectMapper() }

    single { KubernetesProbeManager() }

    val jacksonSerializer = buildJacksonSerializer()

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = jacksonSerializer
        }
    }

    val proxiedHttpClient = HttpClient(Apache) {
        if (System.getenv().containsKey("HTTPS_PROXY")) {
            engine {
                proxy = ProxyBuilder.http(System.getenv("HTTPS_PROXY"))
            }
        }

        install(JsonFeature) {
            serializer = jacksonSerializer
        }
    }

    single { httpClient }
    single(qualifier = StringQualifier("proxyHttpClient")) {proxiedHttpClient}
}

// utils

@KtorExperimentalAPI
fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format(
        "jdbc:postgresql://%s:%s/%s",
        this.property("database.host").getString(),
        this.property("database.port").getString(),
        this.property("database.name").getString()
    )
}
