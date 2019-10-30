package no.nav.syfo.mapping

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.InntektsmeldingDto
import java.time.LocalDate
import java.time.LocalDateTime


fun toInntektsmeldingDTO(inntektsmelding : Inntektsmelding) : InntektsmeldingDto {
    return InntektsmeldingDto(
            aktorId = inntektsmelding.aktorId ?: "",
            sakId = inntektsmelding.sakId ?: "",
            journalpostId = inntektsmelding.journalpostId,
            arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr,
            orgnummer = inntektsmelding.arbeidsgiverOrgnummer
    )
}

fun toInntektsmelding(dto: InntektsmeldingDto) : Inntektsmelding {
    return Inntektsmelding(
            id = dto.uuid!!,
            fnr = dto.arbeidsgiverPrivat ?: "",
            sakId = dto.sakId,
            aktorId = dto.aktorId,
            arbeidsgiverOrgnummer = dto.orgnummer,
            arbeidsgiverPrivatFnr = null,
            arbeidsforholdId = null,
            journalpostId = dto.journalpostId,
            arsakTilInnsending = "?",
            journalStatus = JournalStatus.MIDLERTIDIG,
            arbeidsgiverperioder = ArrayList(),
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
