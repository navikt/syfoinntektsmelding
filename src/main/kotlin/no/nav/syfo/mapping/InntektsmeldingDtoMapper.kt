package no.nav.syfo.mapping

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.ArbeidsgiverperiodeDto
import no.nav.syfo.dto.InntektsmeldingDto
import java.time.LocalDate


fun toInntektsmeldingDTO(inntektsmelding : Inntektsmelding) : InntektsmeldingDto {
    val dto = InntektsmeldingDto(
            aktorId = inntektsmelding.aktorId ?: "",
            sakId = inntektsmelding.sakId ?: "",
            journalpostId = inntektsmelding.journalpostId,
            arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr,
            orgnummer = inntektsmelding.arbeidsgiverOrgnummer,
            behandlet = inntektsmelding.mottattDato
    )
    inntektsmelding.arbeidsgiverperioder.forEach { p -> dto.leggtilArbeidsgiverperiode(p.fom, p.tom) }
    return dto
}

fun toInntektsmelding(dto: InntektsmeldingDto) : Inntektsmelding {
    return Inntektsmelding(
            id = dto.uuid!!,
            fnr = dto.arbeidsgiverPrivat ?: "",
            sakId = dto.sakId,
            aktorId = dto.aktorId,
            arbeidsgiverOrgnummer = dto.orgnummer,
            arbeidsgiverPrivatFnr = dto.arbeidsgiverPrivat,
            arbeidsforholdId = null,
            journalpostId = dto.journalpostId,
            arsakTilInnsending = "?",
            journalStatus = JournalStatus.MIDLERTIDIG,
            arbeidsgiverperioder = mapPerioder(dto.arbeidsgiverperioder),
            beregnetInntekt = null,
            refusjon = Refusjon(),
            endringerIRefusjon = ArrayList(),
            opphørAvNaturalYtelse = ArrayList(),
            gjenopptakelserNaturalYtelse = ArrayList(),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "???",
            feriePerioder = ArrayList(),
            førsteFraværsdag = LocalDate.now(),
            mottattDato = dto.behandlet!!
    )
}

fun mapPerioder(perioder : List<ArbeidsgiverperiodeDto>): List<Periode> {
    return perioder.map{ p -> mapPeriode(p) }
}

// TODO Fjern null sjekken Morten
fun mapPeriode(p : ArbeidsgiverperiodeDto) : Periode{
    return Periode(p.fom!!, p.tom!!)
}
