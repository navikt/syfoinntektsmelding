package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.JournalConsumer;
import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.util.Metrikk;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static no.nav.syfo.domain.InngaaendeJournal.MIDLERTIDIG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JournalpostServiceTest {

    @Mock
    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    @Mock
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    @Mock
    private JournalConsumer journalConsumer;
    @Mock
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;
    @Mock
    private Metrikk metrikk;

    @InjectMocks
    private JournalpostService journalpostService;

    @Test
    public void ferdigstillJournalpost() {
        InngaaendeJournal journal = InngaaendeJournal.builder().dokumentId("dokumentId").status("MIDLERTIDIG").build();
        when(inngaaendeJournalConsumer.hentDokumentId("journalpostId")).thenReturn(journal);

        journalpostService.ferdigstillJournalpost(
                "saksId",
                Inntektsmelding.builder()
                        .fnr("fnr")
                        .arbeidsgiverOrgnummer(Optional.of("orgnummer"))
                        .journalpostId("journalpostId")
                        .arbeidsforholdId(null)
                        .arsakTilInnsending("Ny")
                        .status(MIDLERTIDIG)
                        .build());

        verify(behandlendeEnhetConsumer).hentBehandlendeEnhet("fnr");
        verify(inngaaendeJournalConsumer).hentDokumentId("journalpostId");
        verify(behandleInngaaendeJournalConsumer).oppdaterJournalpost(any());
        verify(behandleInngaaendeJournalConsumer).ferdigstillJournalpost(any());
    }

    @Test
    public void hentInntektsmelding() {
        InngaaendeJournal journal = InngaaendeJournal.builder().dokumentId("dokumentId").status("MIDLERTIDIG").build();
        when(inngaaendeJournalConsumer.hentDokumentId("journalpostId")).thenReturn(journal);
        when(journalConsumer.hentInntektsmelding("journalpostId", journal))
                .thenReturn(Inntektsmelding
                        .builder()
                        .arbeidsgiverOrgnummer(Optional.of("orgnummer"))
                        .orgnummerPrivatperson(Optional.empty())
                        .fnr("fnr")
                        .journalpostId("journalpostId")
                        .status("MIDLERTIDIG")
                        .build());

        Inntektsmelding inntektsmelding = journalpostService.hentInntektsmelding("journalpostId");

        assertThat(inntektsmelding.getFnr()).isEqualTo("fnr");
        assertThat(inntektsmelding.getOrgnummerPrivatperson()).isEqualTo(Optional.empty());
    }
}
