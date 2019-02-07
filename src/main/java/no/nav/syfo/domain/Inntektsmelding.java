package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@Getter
public class Inntektsmelding {
    String fnr;
    Optional<String> arbeidsgiverOrgnummer;
    Optional<String> orgnummerPrivatperson;
    String journalpostId;
    String arbeidsforholdId;
    String arsakTilInnsending;
    String status;
    List<Periode> arbeidsgiverperioder;
}
