package no.nav.syfo.consumer.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.helpers.retry
import log
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.config.SakClientConfig
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@KtorExperimentalAPI
@Component
class SakClient constructor(
        val config: SakClientConfig,
        val tokenConsumer: TokenConsumer
) {

    private val log = log()
    private val httpClient = buildClient()

    private fun buildClient(): HttpClient {
        return HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
                expectSuccess = false
            }
        }
    }

    suspend fun opprettSak(pasientAktoerId: String, msgId: String): SakResponse = retry("opprett_sak") {
        httpClient.post<SakResponse>(config.url) {
            contentType(ContentType.Application.Json)
            header("X-Correlation-ID", msgId)
            header("Authorization", "Bearer ${tokenConsumer.getToken()}")
            body = OpprettSakRequest(
                    tema = "SYK",
                    applikasjon = "FS22",
                    aktoerId = pasientAktoerId,
                    orgnr = null,
                    fagsakNr = null
            )
        }
    }

    private suspend fun finnSak(pasientAktoerId: String, msgId: String): List<SakResponse>? = retry("finn_sak") {
        httpClient.get<List<SakResponse>?>(config.url) {
            contentType(ContentType.Application.Json)
            header("X-Correlation-ID", msgId)
            header("Authorization", "Bearer ${tokenConsumer.getToken()}")
            parameter("tema", "SYM")
            parameter("aktoerId", pasientAktoerId)
            parameter("applikasjon", "FS22")
        }
    }

    suspend fun finnEllerOpprettSak(sykmeldingsId: String, aktorId: String, loggingMeta: LoggingMeta): String {
        val finnSakRespons = finnSak(aktorId, sykmeldingsId)

        val sakIdFraRespons = finnSakRespons?.sortedBy { it.opprettetTidspunkt }?.lastOrNull()?.id?.toString()
        return if (sakIdFraRespons == null) {
            val opprettSakRespons = opprettSak(aktorId, sykmeldingsId)
            log.info("Opprettet en sak med sakid {}, {}", opprettSakRespons.id.toString(), fields(loggingMeta))

            opprettSakRespons.id.toString()
        } else {
            log.info("Fant en sak med sakid {}, {}", sakIdFraRespons, fields(loggingMeta))
            sakIdFraRespons
        }
    }
}

data class OpprettSakRequest(
        val tema: String,
        val applikasjon: String,
        val aktoerId: String,
        val orgnr: String?,
        val fagsakNr: String?
)

data class SakResponse(
        val id: Long,
        val tema: String,
        val aktoerId: String,
        val orgnr: String?,
        val fagsakNr: String?,
        val applikasjon: String,
        val opprettetAv: String,
        val opprettetTidspunkt: ZonedDateTime
)
