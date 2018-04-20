package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;

@Value
@Getter
@Builder
public class Oppgave {
    @NonNull
    String beskrivelse;
    @NonNull
    String gsakSaksid;
    @NonNull
    String journalpostId;
    @NonNull
    LocalDate aktivTil;

    String oppgaveId;
    String geografiskTilknytning;
    String behandlendeEnhetId;
}
