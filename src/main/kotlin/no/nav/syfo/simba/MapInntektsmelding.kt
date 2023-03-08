package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.felles.inntektsmelding.db.InntektsmeldingDokument
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Refusjon
import java.math.BigDecimal
import java.time.LocalDateTime

fun mapInntektsmelding(arkivreferanse: String, aktorId: String, journalpostId: String, imd: InntektsmeldingDokument): Inntektsmelding {
    return Inntektsmelding(
        "",
        imd.identitetsnummer,
        imd.orgnrUnderenhet,
        null,
        null,
        null,
        journalpostId,
        imd.årsakInnsending.name,
        JournalStatus.FERDIGSTILT,
        imd.arbeidsgiverperioder.map { t-> Periode(t.fom,t.tom) },
        BigDecimal(imd.beregnetInntekt),
        Refusjon(BigDecimal(imd.refusjon.refusjonPrMnd?: 0.0 )),
        emptyList(),
        imd.naturalytelser?.map{
            OpphoerAvNaturalytelse(
                no.nav.syfo.domain.inntektsmelding.Naturalytelse.valueOf(it.naturalytelse.name),
                it.dato,
                BigDecimal(it.beløp)
            )
        } ?: emptyList(),
        emptyList(),
        Gyldighetsstatus.GYLDIG,
        arkivreferanse,
        emptyList(),
        imd.bestemmendeFraværsdag,
        LocalDateTime.now(),
        "",
        aktorId,
        "",
        AvsenderSystem("NAV_NO", "1.0"),
        null,
        Kontaktinformasjon("Ukjent", "n/a"),
        LocalDateTime.now(),
        BigDecimal(0),
        null
    )
}
