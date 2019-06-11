package no.nav.syfo.service

import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumer
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.Inntektsmelding
import no.nav.syfo.util.Metrikk
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

import java.util.Optional

import no.nav.syfo.domain.InngaaendeJournal.MIDLERTIDIG
import no.nav.syfo.domain.InngaendeJournalpost
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(MockitoJUnitRunner::class)
class JournalpostServiceTest {

    @Mock
    private val inngaaendeJournalConsumer: InngaaendeJournalConsumer? = null
    @Mock
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer? = null
    @Mock
    private val journalConsumer: JournalConsumer? = null
    @Mock
    private val behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer? = null
    @Mock
    private val metrikk: Metrikk? = null

    @InjectMocks
    private val journalpostService: JournalpostService? = null

    @Test
    fun ferdigstillJournalpost() {
        val journal = InngaaendeJournal.builder().dokumentId("dokumentId").status("MIDLERTIDIG").build()
        `when`(inngaaendeJournalConsumer!!.hentDokumentId("journalpostId")).thenReturn(journal)

        journalpostService!!.ferdigstillJournalpost(
            "saksId",
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                journalpostId = "journalpostId",
                arbeidsforholdId = null,
                arsakTilInnsending = "Ny",
                status = MIDLERTIDIG)
        )

        verify<BehandlendeEnhetConsumer>(behandlendeEnhetConsumer).hentBehandlendeEnhet("fnr")
        verify(inngaaendeJournalConsumer).hentDokumentId("journalpostId")
        verify<BehandleInngaaendeJournalConsumer>(behandleInngaaendeJournalConsumer).oppdaterJournalpost(any<InngaendeJournalpost>())
        verify<BehandleInngaaendeJournalConsumer>(behandleInngaaendeJournalConsumer).ferdigstillJournalpost(any<InngaendeJournalpost>())
    }

    @Test
    fun hentInntektsmelding() {
        val journal = InngaaendeJournal.builder().dokumentId("dokumentId").status("MIDLERTIDIG").build()
        `when`(inngaaendeJournalConsumer!!.hentDokumentId("journalpostId")).thenReturn(journal)
        `when`(journalConsumer!!.hentInntektsmelding("journalpostId", journal))
            .thenReturn(
                Inntektsmelding(
                    arbeidsgiverOrgnummer = "orgnummer",
                    arbeidsgiverPrivat = null,
                    fnr = "fnr",
                    journalpostId = "journalpostId",
                    status = "MIDLERTIDIG",
                    arsakTilInnsending = ""
                )
            )

        val (fnr, _, arbeidsgiverPrivat) = journalpostService!!.hentInntektsmelding("journalpostId")

        assertThat(fnr).isEqualTo("fnr")
        assertThat(arbeidsgiverPrivat).isNull()
    }
}
