package no.nav.syfo.repository;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import no.nav.syfo.domain.Periode;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@Getter
public class InntektsmeldingMeta {
    private String uuid;
    private String aktorId;
    private String sakId;
    private String journalpostId;
    private String orgnummer;
    private String arbeidsgiverPrivat;
    private LocalDateTime behandlet;
    private List<Periode> arbeidsgiverperioder;
}
