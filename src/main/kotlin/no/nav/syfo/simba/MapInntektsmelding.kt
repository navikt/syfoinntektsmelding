package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Bonus
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Feilregistrert
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Ferie
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Ferietrekk
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.InntektEndringAarsak
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.NyStilling
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.NyStillingsprosent
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Nyansatt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Permisjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Permittering
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Sykefravaer
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Tariffendring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.VarigLonnsendring
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
import no.nav.syfo.domain.inntektsmelding.SpinnInntektEndringAarsak
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Inntektsmelding as InntektmeldingSimba

object Avsender {
    val NAV_NO = "NAV_NO"
    val NAV_NO_SELVBESTEMT = NAV_NO + "_SELVBESTEMT"
    val VERSJON = "1.0"
}

fun mapInntektsmelding(arkivreferanse: String, aktorId: String, journalpostId: String, im: InntektmeldingSimba, selvbestemt: Boolean = false): Inntektsmelding {
    val avsenderSystem = if (selvbestemt) {
        Avsender.NAV_NO_SELVBESTEMT
    } else {
        Avsender.NAV_NO
    }
    return Inntektsmelding(
        id = "",
        vedtaksperiodeId = im.vedtaksperiodeId,
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
        inntektsdato = im.inntektsdato,
        refusjon = Refusjon(im.refusjon.refusjonPrMnd.orDefault(0.0).toBigDecimal(), im.refusjon.refusjonOpphører),
        endringerIRefusjon = im.refusjon.refusjonEndringer?.map { EndringIRefusjon(it.dato, it.beløp?.toBigDecimal()) }.orEmpty(),
        opphørAvNaturalYtelse = im.naturalytelser?.map {
            OpphoerAvNaturalytelse(
                Naturalytelse.valueOf(it.naturalytelse.name),
                it.dato,
                it.beløp.toBigDecimal()
            )
        }.orEmpty(),
        gjenopptakelserNaturalYtelse = emptyList(),
        gyldighetsStatus = Gyldighetsstatus.GYLDIG,
        arkivRefereranse = arkivreferanse,
        feriePerioder = emptyList(),
        førsteFraværsdag = im.bestemmendeFraværsdag,
        mottattDato = im.tidspunkt.toLocalDateTime(),
        sakId = "",
        aktorId = aktorId,
        begrunnelseRedusert = im.fullLønnIArbeidsgiverPerioden?.begrunnelse?.name.orEmpty(),
        avsenderSystem = AvsenderSystem(avsenderSystem, Avsender.VERSJON),
        nærRelasjon = null,
        kontaktinformasjon = Kontaktinformasjon(im.innsenderNavn, im.telefonnummer),
        innsendingstidspunkt = im.tidspunkt.toLocalDateTime(),
        bruttoUtbetalt = im.fullLønnIArbeidsgiverPerioden?.utbetalt?.toBigDecimal(),
        årsakEndring = null,
        rapportertInntekt = im.inntekt?.tilRapportertInntekt()
    )
}

fun Inntekt.tilRapportertInntekt() =
    RapportertInntekt(
        bekreftet = this.bekreftet,
        beregnetInntekt = this.beregnetInntekt,
        endringAarsak = this.endringÅrsak?.aarsak(),
        endringAarsakData = this.endringÅrsak?.tilSpinnInntektEndringAarsak(),
        manueltKorrigert = this.manueltKorrigert
    )

fun InntektEndringAarsak.aarsak(): String =
    this.tilSpinnInntektEndringAarsak()?.aarsak ?: this::class.simpleName ?: "Ukjent"

fun InntektEndringAarsak.tilSpinnInntektEndringAarsak(): SpinnInntektEndringAarsak? =
    when (this) {
        is Permisjon -> SpinnInntektEndringAarsak(aarsak = "Permisjon", perioder = liste.map { Periode(it.fom, it.tom) })
        is Ferie -> SpinnInntektEndringAarsak(aarsak = "Ferie", perioder = liste.map { Periode(it.fom, it.tom) })
        is Ferietrekk -> SpinnInntektEndringAarsak(aarsak = "Ferietrekk")
        is Permittering -> SpinnInntektEndringAarsak(aarsak = "Permittering", perioder = liste.map { Periode(it.fom, it.tom) })
        is Tariffendring -> SpinnInntektEndringAarsak("Tariffendring", gjelderFra = gjelderFra, bleKjent = bleKjent)
        is VarigLonnsendring -> SpinnInntektEndringAarsak("VarigLonnsendring", gjelderFra = gjelderFra)
        is NyStilling -> SpinnInntektEndringAarsak(aarsak = "NyStilling", gjelderFra = gjelderFra)
        is NyStillingsprosent -> SpinnInntektEndringAarsak(aarsak = "NyStillingsprosent", gjelderFra = gjelderFra)
        is Bonus -> SpinnInntektEndringAarsak(aarsak = "Bonus")
        is Sykefravaer -> SpinnInntektEndringAarsak(aarsak = "Sykefravaer", perioder = liste.map { Periode(it.fom, it.tom) })
        is Nyansatt -> SpinnInntektEndringAarsak(aarsak = "Nyansatt")
        is Feilregistrert -> SpinnInntektEndringAarsak(aarsak = "Feilregistrert")
        else -> null
    }
