package no.nav.syfo.consumer.ws;

import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class BehandleInngaaendeJournalConsumerTest {

    @Mock
    private BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1;

    @InjectMocks
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;

    @Test
    public void ferdigstillJournalpost() throws Exception {
        final String behandlendeEngetId = "behandlendeEngetId";
        final String journalpostId = "journalpostId";

        ArgumentCaptor<WSFerdigstillJournalfoeringRequest> captor = ArgumentCaptor.forClass(WSFerdigstillJournalfoeringRequest.class);

        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(InngaendeJournalpost.builder()
                .behandlendeEnhetId(behandlendeEngetId)
                .dokumentId("dokumentId")
                .journalpostId(journalpostId)
                .build()
        );
        verify(behandleInngaaendeJournalV1).ferdigstillJournalfoering(captor.capture());

        assertThat(captor.getValue().getEnhetId()).isEqualTo(behandlendeEngetId);
        assertThat(captor.getValue().getJournalpostId()).isEqualTo(journalpostId);
    }

    @Test
    public void oppdaterJournalpostMedPrivatAvsender() throws OppdaterJournalpostSikkerhetsbegrensning, OppdaterJournalpostOppdateringIkkeMulig, OppdaterJournalpostUgyldigInput, OppdaterJournalpostJournalpostIkkeInngaaende, OppdaterJournalpostObjektIkkeFunnet {
        InngaendeJournalpost inngaendeJournalpost = InngaendeJournalpost.builder()
                .fnr("fnr")
                .gsakId("saksID")
                .behandlendeEnhetId("enhet")
                .arbeidsgiverOrgnummer(Optional.empty())
                .dokumentId("dokumentId")
                .journalpostId("journalpostId")
                .arbeidsgiverPrivat(Optional.of("10101033333"))
                .build();

        ArgumentCaptor<WSOppdaterJournalpostRequest> captor = ArgumentCaptor.forClass(WSOppdaterJournalpostRequest.class);

        behandleInngaaendeJournalConsumer.oppdaterJournalpost(inngaendeJournalpost);

        verify(behandleInngaaendeJournalV1).oppdaterJournalpost(captor.capture());
        assertThat(captor.getValue().getInngaaendeJournalpost().getAvsender().getAvsenderId()).isEqualTo("10101033333");
    }
}
