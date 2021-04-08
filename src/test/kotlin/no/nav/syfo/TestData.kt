package testutil

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


val FØRSTE_JANUAR = LocalDate.of(2019, 1, 1)
val FØRSTE_FEBRUAR = LocalDate.of(2019, 2, 1)
val validIdentitetsnummer = "20015001543"
val validOrgNr = "917404437"
val BEHANDLET_DATO = LocalDateTime.of(2021, 6, 23, 12, 0,0)

val grunnleggendeInntektsmelding = Inntektsmelding(
    id = "ID",
    fnr = "12345678901",
    arbeidsgiverOrgnummer = "1234",
    journalpostId = "123",
    arsakTilInnsending = "TEST",
    journalStatus = JournalStatus.ENDELIG,
    arbeidsgiverperioder = listOf(Periode(FØRSTE_JANUAR, FØRSTE_FEBRUAR)),
    arkivRefereranse = "AR123",
    førsteFraværsdag = LocalDate.of(2019, 10, 5),
    mottattDato = LocalDate.of(2019, 10, 25).atStartOfDay()
)

val inntektsmeldingEntitet = InntektsmeldingEntitet(
    uuid =  "UUID",
    aktorId =  validIdentitetsnummer,
    sakId =  "987",
    journalpostId =  "",
    orgnummer = validOrgNr,
    arbeidsgiverPrivat = null,
    behandlet = BEHANDLET_DATO,
    data = null
)
