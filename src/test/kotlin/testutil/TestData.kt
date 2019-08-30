package testutil

import no.nav.syfo.domain.Inntektsmelding
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import java.time.LocalDate


val FØRSTE_JANUAR = LocalDate.of(2019, 1, 1)
val FØRSTE_FEBRUAR = LocalDate.of(2019, 2, 1)


val grunnleggendeInntektsmelding = Inntektsmelding(
        fnr = "12345678901",
        arbeidsgiverOrgnummer = "1234",
        journalpostId = "123",
        arsakTilInnsending = "TEST",
        status = JournalStatus.ENDELIG,
        arbeidsgiverperioder = listOf(Periode(FØRSTE_JANUAR, FØRSTE_FEBRUAR))
)