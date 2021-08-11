package no.nav.syfo.consumer.ws

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson
import no.nav.syfo.behandling.BehandlendeEnhetFeiletException
import no.nav.syfo.behandling.FinnBehandlendeEnhetListeUgyldigInputException
import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.consumer.rest.norg.ArbeidsfordelingResponse
import no.nav.syfo.consumer.rest.norg.Norg2Client
import no.nav.syfo.util.Metrikk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlendeEnhetConsumerTest {

    var pdlClient = mockk<PdlClient>(relaxed = true)
    var norg2Client = mockk<Norg2Client>(relaxed = true)
    var metrikk = mockk<Metrikk>(relaxed = true)

    val TIDSPUNKT = LocalDate.of(2021, 7, 1)
    val FNR = "123456789"
    val UUID = "abcdefgh"
    val DISKRESJONSKODE = "SPFO"
    val ENHET_NR = "enhet_nr_007"

    @Test
    fun skal_finne_enhetsnr() {
        every {
            pdlClient.fullPerson(FNR)
        } returns buildPdlHentFullPerson(DISKRESJONSKODE)
        val arbeidsfordelinger = listOf<ArbeidsfordelingResponse>(buildArbeidsfordelingResponse(ENHET_NR, LocalDate.of(2000,1, 1), LocalDate.of(2030,1,1)))
        every {
            runBlocking {
                norg2Client.hentAlleArbeidsfordelinger(any(), any())
            }
        } returns arbeidsfordelinger
        val enhet = BehandlendeEnhetConsumer(pdlClient, norg2Client, metrikk).hentBehandlendeEnhet(FNR, UUID, TIDSPUNKT)
        Assertions.assertThat(enhet).isEqualTo(ENHET_NR)
    }

    @Test
    fun har_ingen_gyldige_perioder() {
        every {
            pdlClient.fullPerson(FNR)
        } returns buildPdlHentFullPerson(DISKRESJONSKODE)
        val arbeidsfordelinger = listOf<ArbeidsfordelingResponse>(buildArbeidsfordelingResponse(ENHET_NR, LocalDate.of(1900,1,1), LocalDate.of(2000,1,1)))
        every {
            runBlocking {
                norg2Client.hentAlleArbeidsfordelinger(any(), any())
            }
        } returns arbeidsfordelinger
        assertThatThrownBy {
            BehandlendeEnhetConsumer(pdlClient, norg2Client, metrikk).hentBehandlendeEnhet(FNR, UUID, TIDSPUNKT)
        }.isInstanceOf(BehandlendeEnhetFeiletException::class.java)
    }

    @Test
    fun skal_finne_aktiv_arbeidsfordeling() {
        val fordelinger = listOf(buildArbeidsfordelingResponse(ENHET_NR, LocalDate.of(2021,1,1), LocalDate.of(2022,1,1)))
        val enhet = finnAktivBehandlendeEnhet(fordelinger, "", TIDSPUNKT)
        Assertions.assertThat(enhet).isEqualTo(ENHET_NR)
    }

    @Test
    fun skal_ikke_finne_aktiv_arbeidsfordeling() {
        val fordelinger = listOf(buildArbeidsfordelingResponse(ENHET_NR, LocalDate.of(2020,1,1), LocalDate.of(2021,4,1)))
        assertThatThrownBy {
            finnAktivBehandlendeEnhet(fordelinger, "", TIDSPUNKT)
        }.isInstanceOf(IngenAktivEnhetException::class.java)
    }

    fun buildArbeidsfordelingResponse(enhetNr: String, fra: LocalDate, til: LocalDate): ArbeidsfordelingResponse {
        return ArbeidsfordelingResponse(
            behandlingstema = "behandlingstema",
            behandlingstype= "behandlingstype",
            diskresjonskode= "diskresjonskode",
            enhetId= 0,
            enhetNavn= "enhetNavn",
            enhetNr= enhetNr,
            geografiskOmraade= "geografiskOmraade",
            gyldigFra= fra,
            gyldigTil= til,
            id= 0,
            oppgavetype= "oppgavetype",
            skalTilLokalkontor= true,
            tema= "tema",
            temagruppe= "temagruppe"
        )
    }

    fun buildPdlHentFullPerson(diskresjonskode: String): PdlHentFullPerson {
        return PdlHentFullPerson(
            hentPerson = PdlHentFullPerson.PdlFullPersonliste(
                navn = emptyList(),
                foedsel = emptyList(),
                doedsfall = emptyList(),
                adressebeskyttelse = listOf<PdlHentFullPerson.PdlFullPersonliste.PdlAdressebeskyttelse>(
                    PdlHentFullPerson.PdlFullPersonliste.PdlAdressebeskyttelse(diskresjonskode)
                ),
                statsborgerskap = emptyList(),
                bostedsadresse = emptyList(),
                kjoenn = emptyList()
            ),
            hentGeografiskTilknytning = PdlHentFullPerson.PdlGeografiskTilknytning(
                gtBydel = "Oslo",
                gtKommune = "",
                gtType = PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.KOMMUNE,
                gtLand = ""
            ),
            hentIdenter = null
        )
    }

}
