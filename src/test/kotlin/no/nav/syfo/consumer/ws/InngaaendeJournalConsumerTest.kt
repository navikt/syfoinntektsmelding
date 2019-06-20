package no.nav.syfo.consumer.ws

import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.WSJournaltilstand.MIDLERTIDIG
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(MockitoJUnitRunner::class)
class InngaaendeJournalConsumerTest {

    @Mock
    private val inngaaendeJournalV1: InngaaendeJournalV1? = null

    @InjectMocks
    private val inngaaendeJournalConsumer: InngaaendeJournalConsumer? = null

    @Test
    @Throws(Exception::class)
    fun hentDokumentId() {
        val dokumentId1 = "dokumentId"
        val journalpostId = "journalpostId"

        `when`(inngaaendeJournalV1!!.hentJournalpost(any())).thenReturn(
            WSHentJournalpostResponse().withInngaaendeJournalpost(
                WSInngaaendeJournalpost()
                    .withHoveddokument(WSDokumentinformasjon().withDokumentId(dokumentId1))
                    .withJournaltilstand(MIDLERTIDIG)
            )
        )
        val captor = ArgumentCaptor.forClass(WSHentJournalpostRequest::class.java)

        val inngaaendeJournal = inngaaendeJournalConsumer!!.hentDokumentId(journalpostId)

        verify(inngaaendeJournalV1).hentJournalpost(captor.capture())

        assertThat(inngaaendeJournal.dokumentId).isEqualTo(dokumentId1)
        assertThat(captor.value.journalpostId).isEqualTo(journalpostId)
    }

}
