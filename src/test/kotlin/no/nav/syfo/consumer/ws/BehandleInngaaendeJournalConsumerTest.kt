package no.nav.syfo.consumer.ws

import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

import java.util.Optional

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.Mockito.verify


@RunWith(MockitoJUnitRunner::class)
class BehandleInngaaendeJournalConsumerTest {

    @Mock
    private val behandleInngaaendeJournalV1: BehandleInngaaendeJournalV1? = null

    @InjectMocks
    private val behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer? = null

    @Test
    @Throws(Exception::class)
    fun ferdigstillJournalpost() {
        val behandlendeEngetId = "behandlendeEngetId"
        val journalpostId = "journalpostId"

        val captor = ArgumentCaptor.forClass(WSFerdigstillJournalfoeringRequest::class.java)

        behandleInngaaendeJournalConsumer!!.ferdigstillJournalpost(
            InngaendeJournalpost(behandlendeEnhetId = behandlendeEngetId, journalpostId = journalpostId, dokumentId = "dokumentId", fnr = "fnr", gsakId = "id")
        )
        verify<BehandleInngaaendeJournalV1>(behandleInngaaendeJournalV1).ferdigstillJournalfoering(captor.capture())

        assertThat(captor.value.enhetId).isEqualTo(behandlendeEngetId)
        assertThat(captor.value.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    @Throws(
        OppdaterJournalpostSikkerhetsbegrensning::class,
        OppdaterJournalpostOppdateringIkkeMulig::class,
        OppdaterJournalpostUgyldigInput::class,
        OppdaterJournalpostJournalpostIkkeInngaaende::class,
        OppdaterJournalpostObjektIkkeFunnet::class
    )
    fun oppdaterJournalpostMedPrivatAvsender() {
        val inngaendeJournalpost = InngaendeJournalpost(
            fnr = "fnr",
            gsakId = "saksID",
            behandlendeEnhetId = "enhet",
            dokumentId = "dokumentId",
            journalpostId = "journalpostId",
            arbeidsgiverPrivat = "10101033333"
        )

        val captor = ArgumentCaptor.forClass(WSOppdaterJournalpostRequest::class.java)

        behandleInngaaendeJournalConsumer!!.oppdaterJournalpost(inngaendeJournalpost)

        verify<BehandleInngaaendeJournalV1>(behandleInngaaendeJournalV1).oppdaterJournalpost(captor.capture())
        assertThat(captor.value.inngaaendeJournalpost.avsender.avsenderId).isEqualTo("10101033333")
    }
}
