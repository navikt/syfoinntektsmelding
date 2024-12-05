package no.nav.syfo.mapping

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.util.UUID

fun toInntektsmeldingEntitet(inntektsmelding: Inntektsmelding): InntektsmeldingEntitet {
    val entitet = InntektsmeldingEntitet(
        uuid = inntektsmelding.id.ifEmpty { UUID.randomUUID().toString() },
        aktorId = inntektsmelding.aktorId ?: "",
        journalpostId = inntektsmelding.journalpostId,
        arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr,
        orgnummer = inntektsmelding.arbeidsgiverOrgnummer,
        behandlet = inntektsmelding.mottattDato
    )
    inntektsmelding.arbeidsgiverperioder.forEach { p -> entitet.leggtilArbeidsgiverperiode(p.fom, p.tom) }
    return entitet
}

fun toInntektsmelding(inntektsmeldingEntitet: InntektsmeldingEntitet, objectMapper: ObjectMapper): Inntektsmelding {
    val im = objectMapper.readValue(inntektsmeldingEntitet.data, Inntektsmelding::class.java)
    if (im.journalStatus == JournalStatus.MIDLERTIDIG) {
        im.journalStatus = JournalStatus.MOTTATT
    }
    return im
}
