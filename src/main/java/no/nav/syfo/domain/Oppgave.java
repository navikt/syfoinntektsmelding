package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDate;

@Value
@Getter
@Builder
public class Oppgave {
    String oppgaveId;
    String beskrivelse;
    String gsakSaksid;
    String journalpostId;
    LocalDate aktivTil;
    String behandlendeEnhetId;
    String geografiskTilknytning;
    String status;
}
