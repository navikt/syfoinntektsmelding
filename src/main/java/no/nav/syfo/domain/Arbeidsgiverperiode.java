package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
@Getter
public class Arbeidsgiverperiode {
    LocalDate fom;
    LocalDate tom;
}
