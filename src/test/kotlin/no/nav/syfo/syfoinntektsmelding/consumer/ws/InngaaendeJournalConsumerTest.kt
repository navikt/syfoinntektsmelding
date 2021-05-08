package no.nav.syfo.syfoinntektsmelding.consumer.ws


import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinformasjon
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.InngaaendeJournalpost
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journaltilstand
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import javax.xml.datatype.DatatypeFactory


class InngaaendeJournalConsumerTest {

    private val inngaaendeJournalV1 = mockk<InngaaendeJournalV1>(relaxed = true)

    private val inngaaendeJournalConsumer = InngaaendeJournalConsumer(inngaaendeJournalV1)

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

        val journalpostResponse = HentJournalpostResponse()
        journalpostResponse.inngaaendeJournalpost = ijp

        every { inngaaendeJournalV1.hentJournalpost(any()) } returns journalpostResponse
        val captor = slot<HentJournalpostRequest>()

        val inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(journalpostId)

        verify { inngaaendeJournalV1.hentJournalpost( capture(captor)) }

        assertThat(inngaaendeJournal.dokumentId).isEqualTo(dokumentId1)
        assertThat(captor.captured.journalpostId).isEqualTo(journalpostId)
    }

}
