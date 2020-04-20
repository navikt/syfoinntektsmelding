package no.nav.syfo.service

import any
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumer
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.Metrikk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalDateTime

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
                        arbeidsforholdId = null,
                        journalpostId = "journalpostId",
                        arsakTilInnsending = "Ny",
                        journalStatus = JournalStatus.MIDLERTIDIG,
                        arkivRefereranse = "AR123",
                        førsteFraværsdag = LocalDate.now(),
                        mottattDato = LocalDateTime.now()
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
        `when`(journalConsumer!!.hentInntektsmelding("journalpostId", journal, "AR-1234"))
                .thenReturn(
                        Inntektsmelding(
                                fnr = "fnr",
                                arbeidsgiverOrgnummer = "orgnummer",
                                arbeidsgiverPrivatFnr = null,
                                journalpostId = "journalpostId",
                                arsakTilInnsending = "",
                                journalStatus = JournalStatus.MIDLERTIDIG,
                                arkivRefereranse = "AR123",
                                førsteFraværsdag = LocalDate.now(),
                                mottattDato = LocalDateTime.now()
                        )
                )

        val (_, fnr, _, arbeidsgiverPrivat) = journalpostService!!.hentInntektsmelding("journalpostId", "AR-1234")

        assertThat(fnr).isEqualTo("fnr")
        assertThat(arbeidsgiverPrivat).isNull()
    }
}
