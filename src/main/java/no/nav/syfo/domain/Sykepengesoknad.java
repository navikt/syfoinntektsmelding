package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
@Getter
public class Sykepengesoknad {
    private String uuid;
    private String status;
    private String saksId;
    private String oppgaveId;
    private String journalpostId;
}
