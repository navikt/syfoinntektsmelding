package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.utils.pipe.orDefault
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.EndringIRefusjon
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Refusjon
import java.time.LocalDateTime
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Inntektsmelding as InntektmeldingSimba

fun mapInntektsmelding(arkivreferanse: String, aktorId: String, journalpostId: String, im: InntektmeldingSimba): Inntektsmelding {
    return Inntektsmelding(
        id = "",
        fnr = im.identitetsnummer,
        arbeidsgiverOrgnummer = im.orgnrUnderenhet,
        arbeidsgiverPrivatFnr = null,
        arbeidsgiverPrivatAktørId = null,
        arbeidsforholdId = null,
        journalpostId = journalpostId,
        arsakTilInnsending = im.årsakInnsending.name.lowercase().replaceFirstChar { it.uppercase() },
        journalStatus = JournalStatus.FERDIGSTILT,
        arbeidsgiverperioder = im.arbeidsgiverperioder.map { Periode(it.fom, it.tom) },
        beregnetInntekt = im.beregnetInntekt.toBigDecimal(),
        refusjon = Refusjon(im.refusjon.refusjonPrMnd.orDefault(0.0).toBigDecimal(), im.refusjon.refusjonOpphører),
        endringerIRefusjon = im.refusjon.refusjonEndringer?.map { EndringIRefusjon(it.dato, it.beløp?.toBigDecimal()) }.orEmpty(),
        opphørAvNaturalYtelse = im.naturalytelser?.map {
            OpphoerAvNaturalytelse(
                Naturalytelse.valueOf(it.naturalytelse.name),
                it.dato,
                it.beløp.toBigDecimal()
            )
        } ?: emptyList(),
        gjenopptakelserNaturalYtelse = emptyList(),
        gyldighetsStatus = Gyldighetsstatus.GYLDIG,
        arkivRefereranse = arkivreferanse,
        feriePerioder = emptyList(),
        førsteFraværsdag = im.bestemmendeFraværsdag,
        mottattDato = LocalDateTime.now(),
        sakId = "",
        aktorId = aktorId,
        begrunnelseRedusert = im.fullLønnIArbeidsgiverPerioden?.begrunnelse?.name.orEmpty(),
        avsenderSystem = AvsenderSystem("NAV_NO", "1.0"),
        nærRelasjon = null,
        kontaktinformasjon = Kontaktinformasjon(im.innsenderNavn, im.telefonnummer),
        innsendingstidspunkt = LocalDateTime.now(),
        bruttoUtbetalt = im.fullLønnIArbeidsgiverPerioden?.utbetalt?.toBigDecimal(),
        årsakEndring = null
    )
}
