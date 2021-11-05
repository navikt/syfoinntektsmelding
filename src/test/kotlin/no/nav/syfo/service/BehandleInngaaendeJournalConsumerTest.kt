package no.nav.syfo.service

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.domain.InngaendeJournalpost
import org.junit.jupiter.api.Test

class BehandleInngaaendeJournalConsumerTest {

    private val dokArkivClient = mockk<DokArkivClient>(relaxed = true)

    private val behandleInngaaendeJournalConsumer = BehandleInngaaendeJournalConsumer(dokArkivClient)

    @Test
    @Throws(Exception::class)
    fun ferdigstillJournalpost() {
        val behandlendeEngetId = "behandlendeEngetId"
        val journalpostId = "journalpostId"
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(
            InngaendeJournalpost(behandlendeEnhetId = behandlendeEngetId, journalpostId = journalpostId, dokumentId = "dokumentId", fnr = "fnr", gsakId = "id")
        )
        verify {
            runBlocking {
                dokArkivClient.ferdigstillJournalpost(journalpostId, any())
            }
        }
    }

    @Test
    fun oppdaterJournalpostMedPrivatAvsender() {
        val inngaendeJournalpost = InngaendeJournalpost(
            fnr = "fnr",
            gsakId = "saksID",
            behandlendeEnhetId = "enhet",
            dokumentId = "dokumentId",
            journalpostId = "journalpostId",
            arbeidsgiverPrivat = "10101033333"
        )
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(inngaendeJournalpost)
        verify {
            runBlocking {
                dokArkivClient.oppdaterJournalpost("journalpostId", any(), any(), any(), any())
            }
        }
        // TODO - Asserten under må virke
//        assertThat(captor.captured.inngaaendeJournalpost.avsender.avsenderId).isEqualTo("10101033333")
    }
}
