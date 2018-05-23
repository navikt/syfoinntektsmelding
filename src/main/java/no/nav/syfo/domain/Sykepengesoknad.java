package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
@Getter
public class Sykepengesoknad {
    private String uuid;
    private String status;
    private String saksId;
    private String oppgaveId;
    private String journalpostId;
    private LocalDate fom;
    private LocalDate tom;
}
