package no.nav.syfo.utsattoppgave

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.Refusjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

internal class FinnBehandlingsTemaKtTest {

    @Test
    fun krever_ikke_refusjon_ved_null_beløp() {
        var inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = null, LocalDate.now()), BigDecimal(11000))
        assertEquals("1", finnBehandlingsTema(inntektsmelding))
    }

    @Test
    fun krever_ikke_refusjon_ved_tomt_beløp() {
        var inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(0.2), LocalDate.now()), BigDecimal(11000))
        assertEquals("1", finnBehandlingsTema(inntektsmelding))
    }

    @Test
    fun krever_refusjon_med_dato() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(14000), LocalDate.now()), BigDecimal(11000))
        assertEquals("2", finnBehandlingsTema(inntektsmelding))
    }

    @Test
    fun krever_refusjon_mindre_månedslønn() {
        var inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(17000), LocalDate.now()), BigDecimal(11000))
        assertEquals("3", finnBehandlingsTema(inntektsmelding))
    }

    @Test
    fun vanlig_flyt_normal_lønn() {
        var inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(17000), LocalDate.now()), BigDecimal(17000))
        assertEquals("0", finnBehandlingsTema(inntektsmelding))
    }

    @Test
    fun vanlig_flyt() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(13000), LocalDate.now()), BigDecimal(11000))
        assertEquals("0", finnBehandlingsTema(inntektsmelding))
    }

    fun mockInntektsmelding(refusjon: Refusjon, inntekt: BigDecimal): Inntektsmelding {
        return Inntektsmelding(
            id = "",
            fnr = "",
            arbeidsgiverOrgnummer = "123",
            arbeidsgiverPrivatFnr = "",
            arbeidsgiverPrivatAktørId = "",
            arbeidsforholdId = "",
            journalpostId = "",
            arsakTilInnsending = "",
            journalStatus = JournalStatus.MOTTATT,
            arbeidsgiverperioder = emptyList(),
            beregnetInntekt = inntekt,
            refusjon = refusjon,
            endringerIRefusjon = emptyList(), opphørAvNaturalYtelse = emptyList(),
            gjenopptakelserNaturalYtelse = emptyList(),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "ar123",
            feriePerioder = emptyList(),
            førsteFraværsdag = LocalDate.now(),
            mottattDato = LocalDateTime.now(),
            sakId = "sak123", aktorId = "aktor123",
            begrunnelseRedusert = "grunn",
            avsenderSystem = AvsenderSystem("", ""),
            nærRelasjon = false,
            kontaktinformasjon = Kontaktinformasjon("", ""),
            innsendingstidspunkt = LocalDateTime.now(),
            bruttoUtbetalt = BigDecimal(123),
            årsakEndring = "Ingen"
        )
    }
}
