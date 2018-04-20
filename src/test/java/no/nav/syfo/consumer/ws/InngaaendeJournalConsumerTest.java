package no.nav.syfo.consumer.ws;

import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InngaaendeJournalConsumerTest {

    @Mock
    private InngaaendeJournalV1 inngaaendeJournalV1;

    @InjectMocks
    private InngaaendeJournalConsumer inngaaendeJournalConsumer;

    @Test
    public void hentDokumentId() throws Exception {
        final String dokumentId1 = "dokumentId";
        final String journalpostId = "journalpostId";

        when(inngaaendeJournalV1.hentJournalpost(any())).thenReturn(new WSHentJournalpostResponse().withInngaaendeJournalpost(new WSInngaaendeJournalpost().withHoveddokument(new WSDokumentinformasjon().withDokumentId(dokumentId1))));
        ArgumentCaptor<WSHentJournalpostRequest> captor = ArgumentCaptor.forClass(WSHentJournalpostRequest.class);

        String dokumentId = inngaaendeJournalConsumer.hentDokumentId(journalpostId);

        verify(inngaaendeJournalV1).hentJournalpost(captor.capture());

        assertThat(dokumentId).isEqualTo(dokumentId1);
        assertThat(captor.getValue().getJournalpostId()).isEqualTo(journalpostId);
    }

}