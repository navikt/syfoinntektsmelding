package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.ArbeidsfordelingConsumer;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.syfo.domain.Inntektsmelding;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JournalpostServiceTest {

    @Mock
    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    @Mock
    private PersonConsumer personConsumer;
    @Mock
    private ArbeidsfordelingConsumer arbeidsfordelingConsumer;

    @InjectMocks
    private JournalpostService journalpostService;

    @Test
    public void hentInngaendeJournalpost() {
        final String journalpostId = "journalpostId";
        final String behandlendeEnhetId = "behandlendeEnhetId";
        final String dokumentId = "dokumentId";
        final String geografiskTilknytningId = "geografiskTilknytningId";
        final String fnr = "fnr";
        final String gsakId = "gsakId";
        final String orgnummer = "orgnummer";

        when(inngaaendeJournalConsumer.hentDokumentId(journalpostId)).thenReturn(dokumentId);
        when(personConsumer.hentGeografiskTilknytning(fnr)).thenReturn(geografiskTilknytningId);
        when(arbeidsfordelingConsumer.finnBehandlendeEnhet(geografiskTilknytningId)).thenReturn(behandlendeEnhetId);

        InngaendeJournalpost inngaendeJournalpost = journalpostService.hentInngaendeJournalpost(gsakId, Inntektsmelding.builder().fnr(fnr).arbeidsgiverOrgnummer(orgnummer).journalpostId(journalpostId).build());

        assertThat(inngaendeJournalpost).isEqualTo(InngaendeJournalpost.builder().fnr(fnr).journalpostId(journalpostId).behandlendeEnhetId(behandlendeEnhetId).dokumentId(dokumentId).gsakId(gsakId).arbeidsgiverOrgnummer(orgnummer).build());
    }

}