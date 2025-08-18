package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Bonus
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Feilregistrert
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Ferie
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Ferietrekk
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.InntektEndringAarsak
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding.Type
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.NyStilling
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.NyStillingsprosent
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Nyansatt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Permisjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Permittering
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RefusjonEndring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykefravaer
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Tariffendring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.VarigLoennsendring
import no.nav.helsearbeidsgiver.utils.pipe.orDefault
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.EndringIRefusjon
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.RapportertInntekt
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.domain.inntektsmelding.SpinnInntektEndringAarsak
import no.nav.syfo.domain.inntektsmelding.mapTilMottakskanal
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding as InntektmeldingV1
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Naturalytelse as NaturalytelseV1
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode as PeriodeV1
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Refusjon as RefusjonV1

object Avsender {
    const val NAV_NO = "NAV_NO"
    const val NAV_NO_SELVBESTEMT = NAV_NO + "_SELVBESTEMT"
    const val VERSJON = "1.0"
}

fun mapInntektsmelding(
    arkivreferanse: String,
    aktorId: String,
    journalpostId: String,
    im: InntektmeldingV1,
): Inntektsmelding {
    val (
        avsenderSystem: String,
        forespurt: Boolean,
    ) =
        when (im.type) {
            is Type.Forespurt, is Type.ForespurtEkstern -> (Avsender.NAV_NO to true)
            is Type.Selvbestemt, is Type.Fisker, is Type.UtenArbeidsforhold, is Type.Behandlingsdager -> (Avsender.NAV_NO_SELVBESTEMT to false)
        }
    return Inntektsmelding(
        journalStatus = JournalStatus.FERDIGSTILT,
        sakId = "",
        arkivRefereranse = arkivreferanse,
        id = im.id.toString(),
        aktorId = aktorId,
        journalpostId = journalpostId,
        avsenderSystem = AvsenderSystem(avsenderSystem, Avsender.VERSJON),
        vedtaksperiodeId = im.vedtaksperiodeId,
        fnr = im.sykmeldt.fnr.verdi,
        arbeidsgiverOrgnummer = im.avsender.orgnr.verdi,
        kontaktinformasjon = im.avsender.let { Kontaktinformasjon(it.navn, it.tlf) },
        arbeidsgiverperioder =
            im.agp
                ?.perioder
                ?.map(PeriodeV1::tilPeriode)
                .orEmpty(),
        bruttoUtbetalt =
            im.agp
                ?.redusertLoennIAgp
                ?.beloep
                ?.toBigDecimal(),
        begrunnelseRedusert =
            im.agp
                ?.redusertLoennIAgp
                ?.begrunnelse
                ?.name
                .orEmpty(),
        beregnetInntekt = im.inntekt?.beloep?.toBigDecimal(),
        inntektsdato = im.inntekt?.inntektsdato,
        opphørAvNaturalYtelse =
            im.inntekt
                ?.naturalytelser
                ?.map(NaturalytelseV1::tilNaturalytelse)
                .orEmpty(),
        rapportertInntekt = im.inntekt?.tilRapportertInntekt(),
        refusjon = im.refusjon.tilRefusjon(),
        endringerIRefusjon =
            im.refusjon
                ?.endringer
                ?.map(RefusjonEndring::tilEndringIRefusjon)
                .orEmpty(),
        // Spleis trenger ikke denne datoen lenger for IM-er fra nav.no, da de utleder den selv
        førsteFraværsdag = null,
        arsakTilInnsending = im.aarsakInnsending.name,
        mottattDato = im.mottatt.toLocalDateTime(),
        innsendingstidspunkt = im.mottatt.toLocalDateTime(),
        mottaksKanal = im.type.kanal().mapTilMottakskanal(),
        forespurt = forespurt,
    )
}

private fun NaturalytelseV1.tilNaturalytelse(): OpphoerAvNaturalytelse =
    OpphoerAvNaturalytelse(
        naturalytelse = naturalytelse.name.let(Naturalytelse::valueOf),
        fom = sluttdato,
        beloepPrMnd = verdiBeloep.toBigDecimal(),
    )

private fun Inntekt.tilRapportertInntekt(): RapportertInntekt {
    val spinnEndringAarsaker = endringAarsaker.map { it.tilSpinnInntektEndringAarsak() }
    val spinnEndringAarsak = spinnEndringAarsaker.firstOrNull()

    return RapportertInntekt(
        bekreftet = true,
        beregnetInntekt = beloep,
        endringAarsak = spinnEndringAarsak?.aarsak,
        endringAarsakData = spinnEndringAarsak,
        endringAarsakerData = spinnEndringAarsaker,
        manueltKorrigert = spinnEndringAarsaker.isNotEmpty(),
    )
}

private fun RefusjonV1?.tilRefusjon(): Refusjon =
    Refusjon(
        beloepPrMnd = this?.beloepPerMaaned.orDefault(0.0).toBigDecimal(),
    )

private fun RefusjonEndring.tilEndringIRefusjon(): EndringIRefusjon =
    EndringIRefusjon(
        endringsdato = startdato,
        beloep = beloep.toBigDecimal(),
    )

private fun InntektEndringAarsak.tilSpinnInntektEndringAarsak(): SpinnInntektEndringAarsak =
    when (this) {
        is Bonus -> SpinnInntektEndringAarsak(aarsak = "Bonus")
        is Feilregistrert -> SpinnInntektEndringAarsak(aarsak = "Feilregistrert")
        is Ferie -> SpinnInntektEndringAarsak(aarsak = "Ferie", perioder = ferier.map(PeriodeV1::tilPeriode))
        is Ferietrekk -> SpinnInntektEndringAarsak(aarsak = "Ferietrekk")
        is NyStilling -> SpinnInntektEndringAarsak(aarsak = "NyStilling", gjelderFra = gjelderFra)
        is NyStillingsprosent -> SpinnInntektEndringAarsak(aarsak = "NyStillingsprosent", gjelderFra = gjelderFra)
        is Nyansatt -> SpinnInntektEndringAarsak(aarsak = "Nyansatt")
        is Permisjon -> SpinnInntektEndringAarsak(aarsak = "Permisjon", perioder = permisjoner.map(PeriodeV1::tilPeriode))
        is Permittering -> SpinnInntektEndringAarsak(aarsak = "Permittering", perioder = permitteringer.map(PeriodeV1::tilPeriode))
        is Sykefravaer -> SpinnInntektEndringAarsak(aarsak = "Sykefravaer", perioder = sykefravaer.map(PeriodeV1::tilPeriode))
        is Tariffendring -> SpinnInntektEndringAarsak(aarsak = "Tariffendring", gjelderFra = gjelderFra, bleKjent = bleKjent)
        is VarigLoennsendring -> SpinnInntektEndringAarsak(aarsak = "VarigLonnsendring", gjelderFra = gjelderFra)
    }

private fun PeriodeV1.tilPeriode(): Periode =
    Periode(
        fom = fom,
        tom = tom,
    )
