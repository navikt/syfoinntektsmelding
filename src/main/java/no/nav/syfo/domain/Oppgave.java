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
    int versjon;
    String beskrivelse;
    LocalDate aktivFra;
    LocalDate aktivTil;
    String oppgavetype;
    String fagomrade;
    String prioritet;
    String ansvarligEnhetId;
    String saksnummer;
    String dokumentId;
    String status;
    String geografiskTilknytning;
}
