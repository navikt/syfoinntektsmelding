package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.felles.inntektsmelding.db.InntektsmeldingDokument
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.Refusjon
import org.apache.avro.LogicalTypes.Decimal
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

fun mapInntektsmelding(imd: InntektsmeldingDokument): Inntektsmelding {
    return Inntektsmelding(
        "",
        imd.identitetsnummer,
        imd.orgnrUnderenhet,
        null,
        null,
        null,
    "", //@TODO Vi trenger  å legge journalpostid til inntektsmelding dokument
        imd.årsakInnsending.name,
        JournalStatus.FERDIGSTILT,
        imd.arbeidsgiverperioder.map { t-> Periode(t.fom,t.tom) },
        BigDecimal(imd.beregnetInntekt),
        Refusjon(BigDecimal(imd.refusjon.refusjonPrMnd?: 0 )),
        emptyList(),
        emptyList(),
        emptyList(),
        Gyldighetsstatus.GYLDIG,
        "",
        emptyList(),
        LocalDate.now(),
        LocalDateTime.now(),
        "",
        "",
        "",
        AvsenderSystem("", ""),
        false,
        Kontaktinformasjon("", ""),
        LocalDateTime.now(),
        BigDecimal(100),
        ""
    )
}
