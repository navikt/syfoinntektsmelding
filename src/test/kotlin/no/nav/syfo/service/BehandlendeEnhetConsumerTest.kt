package no.nav.syfo.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson
import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.client.norg.ArbeidsfordelingResponse
import no.nav.syfo.client.norg.Norg2Client
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
        val arbeidsfordelinger = listOf<ArbeidsfordelingResponse>(buildArbeidsfordelingResponse(ENHET_NR, LocalDate.of(2000, 1, 1), LocalDate.of(2030, 1, 1)))
        every {
            runBlocking {
                norg2Client.hentAlleArbeidsfordelinger(any(), any())
            }
        } returns arbeidsfordelinger
        val enhet = BehandlendeEnhetConsumer(pdlClient, norg2Client, metrikk).hentBehandlendeEnhet(FNR, UUID, TIDSPUNKT)
        Assertions.assertThat(enhet).isEqualTo(ENHET_NR)
    }

    @Test
    fun skal_finne_aktiv_arbeidsfordeling() {
        val fordelinger = listOf(buildArbeidsfordelingResponse(ENHET_NR, LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1), "Aktiv"))
        val enhet = finnAktivBehandlendeEnhet(fordelinger, "")
        Assertions.assertThat(enhet).isEqualTo(ENHET_NR)
    }

    @Test
    fun skal_ikke_finne_aktiv_arbeidsfordeling() {
        val fordelinger = emptyList<ArbeidsfordelingResponse>()
        assertThatThrownBy {
            finnAktivBehandlendeEnhet(fordelinger, "")
        }.isInstanceOf(IngenAktivEnhetException::class.java)
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
