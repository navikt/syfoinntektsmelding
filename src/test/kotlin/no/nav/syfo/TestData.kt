package no.nav.syfo

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.MottaksKanal
import java.time.LocalDate

val FØRSTE_JANUAR: LocalDate = LocalDate.of(2019, 1, 1)
val FØRSTE_FEBRUAR: LocalDate = LocalDate.of(2019, 2, 1)

val grunnleggendeInntektsmelding =
    Inntektsmelding(
        id = "ID",
        fnr = "12345678901",
        sakId = "sakId",
        aktorId = "aktorId",
        arbeidsgiverOrgnummer = "1234",
        journalpostId = "123",
        arsakTilInnsending = "TEST",
        journalStatus = JournalStatus.FERDIGSTILT,
        arbeidsgiverperioder = listOf(Periode(FØRSTE_JANUAR, FØRSTE_FEBRUAR)),
        arkivRefereranse = "AR123",
        førsteFraværsdag = LocalDate.of(2019, 10, 5),
        mottattDato = LocalDate.of(2019, 10, 25).atStartOfDay(),
        avsenderSystem = AvsenderSystem("NAV_NO", "1.0"),
        mottaksKanal = MottaksKanal.NAV_NO,
    )
