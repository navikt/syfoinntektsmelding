package no.nav.syfo.client.norg

import io.ktor.http.*
import io.ktor.util.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.client.buildHttpClientJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Norg2ClientTest {

    @KtorExperimentalAPI
    private lateinit var norgClient: Norg2Client

    private val mockStsClient = mockk<AccessTokenProvider>(relaxed = true)

    @Test
    @KtorExperimentalAPI
    fun hentAlleArbeidsfordelinger() {
        runBlocking {
            norgClient = Norg2Client("url", mockStsClient, buildHttpClientJson(HttpStatusCode.OK, lagResponse()))
            val arbeidsfordelinger = norgClient.hentAlleArbeidsfordelinger(lagRequest(), "123")
            assertThat(arbeidsfordelinger.size).isEqualTo(1)
            assertThat(arbeidsfordelinger[0].gyldigFra).isEqualTo(LocalDate.of(2021, 1, 1))
        }
    }

    private fun lagResponse(): List<ArbeidsfordelingResponse> {
        val a = ArbeidsfordelingResponse(
            behandlingstema = "string",
            behandlingstype = "string",
            diskresjonskode = "string",
            enhetId = 0,
            enhetNavn = "string",
            enhetNr = "string",
            geografiskOmraade = "string",
            gyldigFra = LocalDate.of(2021, 1, 1),
            gyldigTil = LocalDate.of(2021, 11, 1),
            id = 0,
            oppgavetype = "string",
            skalTilLokalkontor = true,
            tema = "string",
            temagruppe = "string"
        )
        return listOf(a)
    }

    private fun lagRequest(): ArbeidsfordelingRequest {
        return ArbeidsfordelingRequest(
            tema = "tema",
            oppgavetype = "INNT"
        )
    }

}
