package no.nav.syfo.syfoinntektsmelding.consumer.ws

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer
import no.nav.syfo.domain.Oppgave
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.OpprettOppgaveRequest
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.OpprettOppgaveResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class OppgavebehandlingConsumerTest {
    private val oppgavebehandlingV3 = mockk<OppgavebehandlingV3>(relaxed = true)

    private val oppgavebehandlingConsumer = OppgavebehandlingConsumer(oppgavebehandlingV3)

    @Test
    fun opprettOppgave() {
        val ooresponse = OpprettOppgaveResponse()
        ooresponse.oppgaveId = "1234"

        every { oppgavebehandlingV3.opprettOppgave(any()) } returns ooresponse
        val captor = slot<OpprettOppgaveRequest>()
        val ansvarligEnhetId = "behandlendeenhet1234"
        val oppgave = Oppgave(
                beskrivelse = "Beskriv beskriv",
                saksnummer = "gsak1234",
                dokumentId = "journalpost1234",
                ansvarligEnhetId = ansvarligEnhetId,
                aktivTil = LocalDate.of(2018, 1, 1)
        )
        oppgavebehandlingConsumer.opprettOppgave("12345678910", oppgave)

        verify { oppgavebehandlingV3.opprettOppgave( capture(captor)) }
        val opprettOppgave = captor.captured.opprettOppgave

        val aktivTil = LocalDate.of(
                opprettOppgave.aktivTil.year,
                opprettOppgave.aktivTil.month,
                opprettOppgave.aktivTil.day
        )

        assertThat(opprettOppgave.brukertypeKode).isEqualTo("PERSON")
        assertThat(opprettOppgave.oppgavetypeKode).isEqualTo("INNT_SYK")
        assertThat(opprettOppgave.fagomradeKode).isEqualTo("SYK")
        assertThat(opprettOppgave.underkategoriKode).isEqualTo("SYK_SYK")
        assertThat(opprettOppgave.prioritetKode).isEqualTo("NORM_SYK")
        assertThat(opprettOppgave.beskrivelse).isEqualTo(oppgave.beskrivelse)
        assertThat(aktivTil).isEqualTo(oppgave.aktivTil)
        assertThat(opprettOppgave.ansvarligEnhetId).isEqualTo(ansvarligEnhetId)
        assertThat(opprettOppgave.dokumentId).isEqualTo(oppgave.dokumentId)
        assertThat(opprettOppgave.saksnummer).isEqualTo(oppgave.saksnummer)
        assertThat(opprettOppgave.oppfolging).isEqualTo("\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" + "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding")
    }
}
