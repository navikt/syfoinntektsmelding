package no.nav.syfo

import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.koin.buildObjectMapper
import no.nav.syfo.repository.buildIM
import java.time.LocalDateTime
import java.util.UUID

object UtsattOppgaveTestData {
    const val FNR = "fnr"
    const val AKTOER_ID = "aktørId"
    const val JOURNALPOST_ID = "journalpostId"
    const val ARKIVREFERANSE = "123"

    val timeout: LocalDateTime = LocalDateTime.of(2023, 4, 6, 9, 0)
    val oppgave =
        UtsattOppgaveEntitet(
            fnr = FNR,
            aktørId = AKTOER_ID,
            journalpostId = JOURNALPOST_ID,
            arkivreferanse = ARKIVREFERANSE,
            timeout = timeout,
            inntektsmeldingId = UUID.randomUUID().toString(),
            tilstand = Tilstand.Utsatt,
            gosysOppgaveId = null,
            oppdatert = null,
            speil = false,
            utbetalingBruker = false,
        )

    val inntektsmeldingEntitet =
        InntektsmeldingEntitet(
            uuid = UUID.randomUUID().toString(),
            aktorId = "aktoerid-123",
            behandlet = LocalDateTime.now(),
            orgnummer = "arb-org-123",
            journalpostId = "jp-123",
            fnr = Fnr("28014026691"),
            data = buildObjectMapper().writeValueAsString(buildIM()),
        )
    val inntektsmeldingEntitetIkkeFravaer =
        inntektsmeldingEntitet.copy(
            data = buildObjectMapper().writeValueAsString(buildIM().copy(begrunnelseRedusert = "IkkeFravaer")),
        )
}
