package no.nav.syfo.consumer.ws;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import no.nav.syfo.domain.Periode;

import java.util.List;
import java.util.Optional;

@Value
@Builder
@Getter
public class XMLInntektsmelding {
    String arbeidsforholdId;
    List<Periode> perioder;
    String arbeidstakerFnr;
    Optional<String> virksomhetsnummer;
    Optional<String> arbeidsgiverPrivat;
    String aarsakTilInnsending;
}
