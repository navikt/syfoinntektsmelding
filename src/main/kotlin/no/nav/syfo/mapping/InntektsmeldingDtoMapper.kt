package no.nav.syfo.mapping

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.time.LocalDate
import kotlin.collections.ArrayList


fun toInntektsmeldingEntitet(inntektsmelding : Inntektsmelding) : InntektsmeldingEntitet {
    val entitet = InntektsmeldingEntitet(
            aktorId = inntektsmelding.aktorId ?: "",
            sakId = inntektsmelding.sakId ?: "",
            journalpostId = inntektsmelding.journalpostId,
            arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr,
            orgnummer = inntektsmelding.arbeidsgiverOrgnummer,
            behandlet = inntektsmelding.mottattDato
    )
    inntektsmelding.arbeidsgiverperioder.forEach { p -> entitet.leggtilArbeidsgiverperiode(p.fom, p.tom) }
    return entitet
}

fun toInntektsmelding(inntektsmeldingEntitet: InntektsmeldingEntitet) : Inntektsmelding {
    return Inntektsmelding(
        id = inntektsmeldingEntitet.uuid,
        fnr = inntektsmeldingEntitet.arbeidsgiverPrivat ?: "",
        arbeidsgiverOrgnummer = inntektsmeldingEntitet.orgnummer,
        arbeidsgiverPrivatFnr = inntektsmeldingEntitet.arbeidsgiverPrivat,
        arbeidsforholdId = null,
        journalpostId = inntektsmeldingEntitet.journalpostId,
        arsakTilInnsending = "?",
        journalStatus = JournalStatus.MOTTATT,
        arbeidsgiverperioder = mapPerioder(inntektsmeldingEntitet.arbeidsgiverperioder), // TODO - Dette feltet må populeres fra database
        beregnetInntekt = null,
        refusjon = Refusjon(),
        endringerIRefusjon = ArrayList(),
        opphørAvNaturalYtelse = ArrayList(),
        gjenopptakelserNaturalYtelse = ArrayList(),
        gyldighetsStatus = Gyldighetsstatus.GYLDIG,
        arkivRefereranse = "???",
        feriePerioder = ArrayList(),
        førsteFraværsdag = LocalDate.now(), // TODO - Dette feltet må populeres fra database
        mottattDato = inntektsmeldingEntitet.behandlet!!,
        sakId = inntektsmeldingEntitet.sakId,
        aktorId = inntektsmeldingEntitet.aktorId,
        innsendingstidspunkt = null,
        bruttoUtbetalt = null,
        årsakEndring = null
    )
}

fun mapPerioder(perioder : List<ArbeidsgiverperiodeEntitet>): List<Periode> {
    return perioder.map{ p -> mapPeriode(p) }
}

fun mapPeriode(p : ArbeidsgiverperiodeEntitet) : Periode{
    return Periode(p.fom, p.tom)
}
