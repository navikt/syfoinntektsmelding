package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.Optional;

@Value
@Builder
@Getter
public class InngaendeJournalpost {
    String fnr;
    String journalpostId;
    String dokumentId;
    String behandlendeEnhetId;
    String gsakId;
    Optional<String> arbeidsgiverOrgnummer;
    Optional<String> orgnummerPrivatperson;
}
