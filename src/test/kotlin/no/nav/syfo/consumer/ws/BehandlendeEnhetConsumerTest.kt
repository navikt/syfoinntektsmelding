package no.nav.syfo.consumer.ws

import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.consumer.rest.norg.ArbeidsfordelingResponse
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlendeEnhetConsumerTest {

    val tidspunkt = LocalDate.of(2021, 7, 1)

    @Test
    fun skal_finne_aktiv_arbeidsfordeling() {
        val fordelinger = listOf(lagFordeling(LocalDate.of(2021,1,1), LocalDate.of(2022,1,1)))
        val enhet = finnAktivBehandlendeEnhet(fordelinger, "", tidspunkt)
        Assertions.assertThat(enhet).isEqualTo("enhetNr")
    }

    @Test
    fun skal_ikke_finne_aktiv_arbeidsfordeling() {
        val fordelinger = listOf(lagFordeling(LocalDate.of(2020,1,1), LocalDate.of(2021,4,1)))
        assertThatThrownBy {
            finnAktivBehandlendeEnhet(fordelinger, "", tidspunkt)
        }.isInstanceOf(IngenAktivEnhetException::class.java)
    }

    private fun lagFordeling(fra: LocalDate, til: LocalDate): ArbeidsfordelingResponse {
        return ArbeidsfordelingResponse(
            behandlingstema = "behandlingstema",
            behandlingstype= "behandlingstype",
            diskresjonskode= "diskresjonskode",
            enhetId= 0,
            enhetNavn= "enhetNavn",
            enhetNr= "enhetNr",
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
}
