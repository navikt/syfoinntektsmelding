package no.nav.syfo.consumer.ws


import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinformasjon
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.InngaaendeJournalpost
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journaltilstand
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import javax.xml.datatype.DatatypeFactory

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

        val ijp = InngaaendeJournalpost()
        ijp.hoveddokument = Dokumentinformasjon()
        ijp.hoveddokument.dokumentId = dokumentId1
        ijp.journaltilstand = Journaltilstand.MIDLERTIDIG
        ijp.forsendelseMottatt = DatatypeFactory.newInstance().newXMLGregorianCalendar()
        // JournalStatus.MIDLERTIDIG

        val journalpostResponse = HentJournalpostResponse()
        journalpostResponse.inngaaendeJournalpost = ijp

        `when`(inngaaendeJournalV1!!.hentJournalpost(any())).thenReturn(
                journalpostResponse
        )
        val captor = ArgumentCaptor.forClass(HentJournalpostRequest::class.java)

        val inngaaendeJournal = inngaaendeJournalConsumer!!.hentDokumentId(journalpostId)

        verify(inngaaendeJournalV1).hentJournalpost(captor.capture())

        assertThat(inngaaendeJournal.dokumentId).isEqualTo(dokumentId1)
        assertThat(captor.value.journalpostId).isEqualTo(journalpostId)
    }

}
