package no.nav.syfo.repository;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
@Getter
public class InntektsmeldingMeta {
    private String uuid;
    private String aktorId;
    private String sakId;
    private String journalpostId;
    private String orgnummer;
    private LocalDate arbeidsgiverperiodeFom;
    private LocalDate arbeidsgiverperiodeTom;
    private LocalDateTime behandlet;
}
