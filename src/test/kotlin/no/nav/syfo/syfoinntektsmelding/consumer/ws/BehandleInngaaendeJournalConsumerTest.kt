package no.nav.syfo.syfoinntektsmelding.consumer.ws

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.*
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.OppdaterJournalpostRequest
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class BehandleInngaaendeJournalConsumerTest {

    private val behandleInngaaendeJournalV1 = mockk<BehandleInngaaendeJournalV1>(relaxed = true)

    private val behandleInngaaendeJournalConsumer = BehandleInngaaendeJournalConsumer(behandleInngaaendeJournalV1)

    @Test
    @Throws(Exception::class)
    fun ferdigstillJournalpost() {
        val behandlendeEngetId = "behandlendeEngetId"
        val journalpostId = "journalpostId"

        val captor = slot<FerdigstillJournalfoeringRequest>()

        behandleInngaaendeJournalConsumer!!.ferdigstillJournalpost(
                InngaendeJournalpost(behandlendeEnhetId = behandlendeEngetId, journalpostId = journalpostId, dokumentId = "dokumentId", fnr = "fnr", gsakId = "id")
        )
        verify { behandleInngaaendeJournalV1.ferdigstillJournalfoering( capture(captor)) }

        assertThat(captor.captured.enhetId).isEqualTo("9999")
        assertThat(captor.captured.journalpostId).isEqualTo(journalpostId)
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

        val captor = slot<OppdaterJournalpostRequest>()

        behandleInngaaendeJournalConsumer!!.oppdaterJournalpost(inngaendeJournalpost)

        verify { behandleInngaaendeJournalV1.oppdaterJournalpost( capture(captor)) }
        assertThat(captor.captured.inngaaendeJournalpost.avsender.avsenderId).isEqualTo("10101033333")
    }
}
