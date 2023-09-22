package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.InntektsmeldingDokument
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.EndringIRefusjon
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Refusjon
import java.math.BigDecimal
import java.time.LocalDateTime

fun mapInntektsmelding(arkivreferanse: String, aktorId: String, journalpostId: String, imd: InntektsmeldingDokument): Inntektsmelding {
    return Inntektsmelding(
        id = "",
        fnr = imd.identitetsnummer,
        arbeidsgiverOrgnummer = imd.orgnrUnderenhet,
        arbeidsgiverPrivatFnr = null,
        arbeidsgiverPrivatAktørId = null,
        arbeidsforholdId = null,
        journalpostId = journalpostId,
        arsakTilInnsending = imd.årsakInnsending.name.lowercase().replaceFirstChar { it.uppercase() },
        journalStatus = JournalStatus.FERDIGSTILT,
        arbeidsgiverperioder = imd.arbeidsgiverperioder.map { t -> Periode(t.fom, t.tom) },
        beregnetInntekt = imd.beregnetInntekt,
        refusjon = Refusjon(imd.refusjon.refusjonPrMnd ?: 0.0.toBigDecimal(), imd.refusjon.refusjonOpphører),
        endringerIRefusjon = imd.refusjon.refusjonEndringer?.map { EndringIRefusjon(it.dato, it.beløp) } ?: emptyList(),
        opphørAvNaturalYtelse = imd.naturalytelser?.map {
            OpphoerAvNaturalytelse(
                no.nav.syfo.domain.inntektsmelding.Naturalytelse.valueOf(it.naturalytelse.value),
                it.dato,
                it.beløp
            )
        } ?: emptyList(),
        gjenopptakelserNaturalYtelse = emptyList(),
        gyldighetsStatus = Gyldighetsstatus.GYLDIG,
        arkivRefereranse = arkivreferanse,
        feriePerioder = emptyList(),
        førsteFraværsdag = imd.bestemmendeFraværsdag,
        mottattDato = LocalDateTime.now(),
        sakId = "",
        aktorId = aktorId,
        begrunnelseRedusert = imd.fullLønnIArbeidsgiverPerioden?.begrunnelse?.value.orEmpty(),
        avsenderSystem = AvsenderSystem("NAV_NO", "1.0"),
        nærRelasjon = null,
        kontaktinformasjon = Kontaktinformasjon(imd.innsenderNavn, imd.telefonnummer),
        innsendingstidspunkt = LocalDateTime.now(),
        bruttoUtbetalt = BigDecimal(0),
        årsakEndring = null
    )
}
