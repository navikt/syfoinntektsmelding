package no.nav.syfo.consumer.ws

import no.nav.syfo.domain.Oppgave
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.OpprettOppgaveResponse
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@RunWith(MockitoJUnitRunner::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class OppgavebehandlingConsumerTest {
    @Mock
    private val oppgavebehandlingV3: OppgavebehandlingV3? = null

    @InjectMocks
    private val oppgavebehandlingConsumer: OppgavebehandlingConsumer? = null

    @Test
    fun opprettOppgave() {
        val ooresponse = OpprettOppgaveResponse()
        ooresponse.oppgaveId = "1234"

        `when`(oppgavebehandlingV3!!.opprettOppgave(any())).thenReturn(ooresponse)
        val captor = ArgumentCaptor.forClass(OpprettOppgaveRequest::class.java)
        val ansvarligEnhetId = "behandlendeenhet1234"
        val oppgave = Oppgave(
                beskrivelse = "Beskriv beskriv",
                saksnummer = "gsak1234",
                dokumentId = "journalpost1234",
                ansvarligEnhetId = ansvarligEnhetId,
                aktivTil = LocalDate.of(2018, 1, 1)
        )
        oppgavebehandlingConsumer!!.opprettOppgave("12345678910", oppgave)

        verify(oppgavebehandlingV3).opprettOppgave(captor.capture())
        val opprettOppgave = captor.value.opprettOppgave

        val aktivTil = LocalDate.of(
                opprettOppgave.aktivTil.getYear(),
                opprettOppgave.aktivTil.getMonth(),
                opprettOppgave.aktivTil.getDay())

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
