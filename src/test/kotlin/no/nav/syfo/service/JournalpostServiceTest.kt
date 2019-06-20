package no.nav.syfo.service

import any
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

import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.domain.JournalStatus
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(MockitoJUnitRunner::class)
class JournalpostServiceTest {

    @Mock
    private lateinit var inngaaendeJournalConsumer: InngaaendeJournalConsumer
    @Mock
    private lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer
    @Mock
    private lateinit var journalConsumer: JournalConsumer
    @Mock
    private lateinit var behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer
    @Mock
    private val metrikk: Metrikk? = null

    @InjectMocks
    private val journalpostService: JournalpostService? = null

    @Test
    fun ferdigstillJournalpost() {
        val journal = InngaaendeJournal(dokumentId = "dokumentId", status = JournalStatus.MIDLERTIDIG)
        `when`(inngaaendeJournalConsumer!!.hentDokumentId("journalpostId")).thenReturn(journal)
        given(behandlendeEnhetConsumer.hentBehandlendeEnhet(anyString())).willReturn("enhet")

        journalpostService!!.ferdigstillJournalpost(
            "saksId",
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                journalpostId = "journalpostId",
                arbeidsforholdId = null,
                arsakTilInnsending = "Ny",
                status = JournalStatus.MIDLERTIDIG
            )
        )

        verify<BehandlendeEnhetConsumer>(behandlendeEnhetConsumer).hentBehandlendeEnhet("fnr")
        verify(inngaaendeJournalConsumer).hentDokumentId("journalpostId")
        verify<BehandleInngaaendeJournalConsumer>(behandleInngaaendeJournalConsumer).oppdaterJournalpost(any())
        verify<BehandleInngaaendeJournalConsumer>(behandleInngaaendeJournalConsumer).ferdigstillJournalpost(any())
    }

    @Test
    fun hentInntektsmelding() {
        val journal = InngaaendeJournal(dokumentId = "dokumentId", status = JournalStatus.MIDLERTIDIG)
        `when`(inngaaendeJournalConsumer!!.hentDokumentId("journalpostId")).thenReturn(journal)
        `when`(journalConsumer!!.hentInntektsmelding("journalpostId", journal))
            .thenReturn(
                Inntektsmelding(
                    arbeidsgiverOrgnummer = "orgnummer",
                    arbeidsgiverPrivat = null,
                    fnr = "fnr",
                    journalpostId = "journalpostId",
                    status = JournalStatus.MIDLERTIDIG,
                    arsakTilInnsending = ""
                )
            )

        val (fnr, _, arbeidsgiverPrivat) = journalpostService!!.hentInntektsmelding("journalpostId")

        assertThat(fnr).isEqualTo("fnr")
        assertThat(arbeidsgiverPrivat).isNull()
    }
}
