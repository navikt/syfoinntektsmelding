package no.nav.syfo.repository;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
@Getter
public class InntektsmeldingMeta {
    private String uuid;
    private String aktorId;
    private String sakId;
    private String orgnummer;
    private LocalDate arbeidsgiverperiodeFom;
    private LocalDate arbeidsgiverperiodeTom;
}
