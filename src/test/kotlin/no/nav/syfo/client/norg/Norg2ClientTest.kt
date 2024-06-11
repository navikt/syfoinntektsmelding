package no.nav.syfo.client.norg

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.buildHttpClientJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Norg2ClientTest {

    private lateinit var norgClient: Norg2Client

    @Test

    fun hentAlleArbeidsfordelinger() {
        runBlocking {
            norgClient = Norg2Client("url", buildHttpClientJson(HttpStatusCode.OK, lagResponse()))
            val arbeidsfordelinger = norgClient.hentAlleArbeidsfordelinger(lagRequest(), "123")
            assertThat(arbeidsfordelinger.size).isEqualTo(1)
            assertThat(arbeidsfordelinger[0].enhetNr).isEqualTo("1234")
        }
    }

    private fun lagResponse(): List<ArbeidsfordelingResponse> {
        val a = ArbeidsfordelingResponse(
            aktiveringsdato = LocalDate.of(2020, 12, 31),
            antallRessurser = 0,
            enhetId = 123456789,
            enhetNr = "1234",
            kanalstrategi = null,
            navn = "NAV Område",
            nedleggelsesdato = null,
            oppgavebehandler = false,
            orgNivaa = "SPESEN",
            orgNrTilKommunaltNavKontor = "",
            organisasjonsnummer = null,
            sosialeTjenester = "",
            status = "Aktiv",
            type = "KO",
            underAvviklingDato = null,
            underEtableringDato = LocalDate.of(2020, 11, 30),
            versjon = 1
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
