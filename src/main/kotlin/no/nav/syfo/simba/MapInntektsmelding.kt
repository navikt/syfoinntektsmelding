package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.Bonus
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Feilregistrert
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Ferie
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Ferietrekk
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.InntektEndringAarsak
import no.nav.helsearbeidsgiver.domene.inntektsmelding.NyStilling
import no.nav.helsearbeidsgiver.domene.inntektsmelding.NyStillingsprosent
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Nyansatt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Permisjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Permittering
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Sykefravaer
import no.nav.helsearbeidsgiver.domene.inntektsmelding.Tariffendring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.VarigLonnsendring
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
import no.nav.syfo.domain.inntektsmelding.RapportertInntekt
import no.nav.syfo.domain.inntektsmelding.Refusjon
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
        // TODO rename 'bestemmendeFraværsdag' -> 'inntektsdato'
        inntektsdato = im.bestemmendeFraværsdag,
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
        førsteFraværsdag = im.foersteFravaersdag(),
        mottattDato = LocalDateTime.now(),
        sakId = "",
        aktorId = aktorId,
        begrunnelseRedusert = im.fullLønnIArbeidsgiverPerioden?.begrunnelse?.name.orEmpty(),
        avsenderSystem = AvsenderSystem("NAV_NO", "1.0"),
        nærRelasjon = null,
        kontaktinformasjon = Kontaktinformasjon(im.innsenderNavn, im.telefonnummer),
        innsendingstidspunkt = LocalDateTime.now(),
        bruttoUtbetalt = im.fullLønnIArbeidsgiverPerioden?.utbetalt?.toBigDecimal(),
        årsakEndring = null,
        rapportertInntekt = im.inntekt?.tilRapportertInntekt()
    )
}

private fun InntektmeldingSimba.foersteFravaersdag(): LocalDate =
    if (arbeidsgiverperioder.isNotEmpty()) arbeidsgiverperioder.maxBy { it.fom }.fom
    else {
        val sisteFravaersperiode = fraværsperioder.maxBy { it.fom }

        egenmeldingsperioder.sortedByDescending { it.fom }
            .fold(listOf(sisteFravaersperiode)) { sammenhengendePerioder, egenmeldingsperiode ->
                val foerstePeriodeISammenhengende = sammenhengendePerioder.last()

                if (egenmeldingsperiode.tom.erRettFoer(foerstePeriodeISammenhengende.fom)) {
                    sammenhengendePerioder.plus(egenmeldingsperiode)
                } else {
                    sammenhengendePerioder
                }
            }
            .last()
            .fom
    }

fun LocalDate.erRettFoer(other: LocalDate): Boolean =
    when (until(other, ChronoUnit.DAYS)) {
        1L -> true
        2L -> dayOfWeek in listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        3L -> dayOfWeek == DayOfWeek.FRIDAY
        else -> false
    }

fun Inntekt.tilRapportertInntekt() =
    RapportertInntekt(
        bekreftet = this.bekreftet,
        beregnetInntekt = this.beregnetInntekt,
        endringAarsak = this.endringÅrsak?.aarsak(),
        manueltKorrigert = this.manueltKorrigert
    )

fun InntektEndringAarsak.aarsak(): String =
    when (this) {
        is Permisjon -> "Permisjon"
        is Ferie -> "Ferie"
        is Ferietrekk -> "Ferietrekk"
        is Permittering -> "Permittering"
        is Tariffendring -> "Tariffendring"
        is VarigLonnsendring -> "VarigLonnsendring"
        is NyStilling -> "NyStilling"
        is NyStillingsprosent -> "NyStillingsprosent"
        is Bonus -> "NyStillingsprosent"
        is Sykefravaer -> "Sykefravaer"
        is Nyansatt -> "Nyansatt"
        is Feilregistrert -> "Feilregistrert"
        else -> this::class.simpleName ?: "Ukjent"
    }
