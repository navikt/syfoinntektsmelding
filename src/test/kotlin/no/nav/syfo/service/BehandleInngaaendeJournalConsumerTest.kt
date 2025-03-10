package no.nav.syfo.service

import io.mockk.coVerifySequence
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.grunnleggendeInntektsmelding
import org.junit.jupiter.api.Test

class BehandleInngaaendeJournalConsumerTest {
    private val dokArkivClient = mockk<DokArkivClient>(relaxed = true)

    private val behandleInngaaendeJournalConsumer = BehandleInngaaendeJournalConsumer(dokArkivClient)

    @Test
    fun ferdigstillJournalpost() {
        val behandlendeEngetId = "behandlendeEngetId"
        val journalpostId = "journalpostId"
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(
            InngaendeJournalpost(
                behandlendeEnhetId = behandlendeEngetId,
                journalpostId = journalpostId,
                dokumentId = "dokumentId",
                fnr = "fnr",
            ),
        )
        verify {
            runBlocking {
                dokArkivClient.ferdigstillJournalpost(journalpostId, any())
            }
        }
    }

    @Test
    fun oppdaterJournalpostMedPrivatAvsender() {
        val inngaendeJournalpost =
            InngaendeJournalpost(
                fnr = "fnr",
                behandlendeEnhetId = "enhet",
                dokumentId = "dokumentId",
                journalpostId = "journalpostId",
            )
        val dokumentTittel = "Inntektsmelding-01.01.2019 - 01.02.2019"
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null), inngaendeJournalpost, false)
        coVerifySequence {
            dokArkivClient.oppdaterJournalpost("journalpostId", match { it.dokumenter!!.first().tittel == dokumentTittel }, any())
        }
        // TODO - Asserten under må virke
//        assertThat(captor.captured.inngaaendeJournalpost.avsender.avsenderId).isEqualTo("10101033333")
    }
}
