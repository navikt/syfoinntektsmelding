package no.nav.syfo.consumer.ws;

import no.nav.syfo.domain.Inntektsmelding;
import no.nav.tjeneste.virksomhet.journal.v2.JournalV2;
import no.nav.tjeneste.virksomhet.journal.v2.WSHentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v2.WSHentDokumentResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.syfo.util.JAXBTest.getInntektsmelding;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class JournalConsumerTest {

    @Mock
    private JournalV2 journalV3;

    @InjectMocks
    private JournalConsumer journalConsumer;

    @Test
    public void hentInntektsmelding() throws Exception {
        when(journalV3.hentDokument(any())).thenReturn(new WSHentDokumentResponse().withDokument(getInntektsmelding().getBytes()));
        ArgumentCaptor<WSHentDokumentRequest> captor = ArgumentCaptor.forClass(WSHentDokumentRequest.class);

        Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding("journalpostId", "dokumentId");

        verify(journalV3).hentDokument(captor.capture());

        assertThat(inntektsmelding.getFnr()).isEqualTo("18018522868");
        assertThat(captor.getValue().getJournalpostId()).isEqualTo("journalpostId");
        assertThat(captor.getValue().getDokumentId()).isEqualTo("dokumentId");
    }

}