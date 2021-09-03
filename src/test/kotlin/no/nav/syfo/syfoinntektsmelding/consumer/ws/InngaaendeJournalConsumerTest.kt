package no.nav.syfo.syfoinntektsmelding.consumer.ws


import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.mockito.ArgumentMatchers.any
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.graphql.model.SafJournalResponse
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.client.saf.model.Dokument
import no.nav.syfo.client.saf.model.Journalpost
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime


class InngaaendeJournalConsumerTest {

    private val safJournalpostClient = mockk<SafJournalpostClient>(relaxed = true)

    private val inngaaendeJournalConsumer = InngaaendeJournalConsumer(safJournalpostClient)

    @Test
    @Throws(Exception::class)
    fun hentDokumentId() {
        val dokumentId1 = "dokumentId"
        val journalpostId = "journalpostId"

        val journalpostResponse = SafJournalResponse(
            journalpost = Journalpost(
                JournalStatus.MIDLERTIDIG,
                datoOpprettet = LocalDateTime.now(),
                dokumenter = listOf(Dokument(dokumentId1))
            ),
            errors = emptyList()
        )

        every {
            safJournalpostClient.getJournalpostMetadata(any())
        } returns journalpostResponse
        val captor = slot<String>()

        val inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(journalpostId)

        verify {
            safJournalpostClient.getJournalpostMetadata( capture(captor) )
        }

        assertThat(inngaaendeJournal.dokumentId).isEqualTo(dokumentId1)
        assertThat(captor.captured).isEqualTo(journalpostId)
    }

}
