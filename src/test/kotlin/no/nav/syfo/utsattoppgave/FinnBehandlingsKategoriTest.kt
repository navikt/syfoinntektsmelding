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

internal class FinnBehandlingsKategoriTest {

    @Test
    fun krever_ikke_refusjon_ved_null_beløp() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = null, LocalDate.now()), BigDecimal(11000))
        assertEquals(BehandlingsKategori.IKKE_REFUSJON, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = false, gjelderUtland = false))
    }

    @Test
    fun krever_ikke_refusjon_ved_tomt_beløp() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(0.2), LocalDate.now()), BigDecimal(11000))
        assertEquals(BehandlingsKategori.IKKE_REFUSJON, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = false, gjelderUtland = false))
    }

    @Test
    fun krever_refusjon_med_dato() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(14000), LocalDate.now()), BigDecimal(11000))
        assertEquals(BehandlingsKategori.REFUSJON_MED_DATO, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = false, gjelderUtland = false))
    }

    @Test
    fun krever_refusjon_mindre_månedslønn() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(10000), null), BigDecimal(11000))
        assertEquals(BehandlingsKategori.REFUSJON_LITEN_LØNN, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = false, gjelderUtland = false))
    }

    @Test
    fun vanlig_flyt_normal_lønn() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(17000), null), BigDecimal(17000))
        assertEquals(BehandlingsKategori.REFUSJON_UTEN_DATO, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = false, gjelderUtland = false))
    }

    @Test
    fun bestrider_sykemelding() {
        val inntektsmelding =
            mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(17000), null), BigDecimal(17000)).copy(begrunnelseRedusert = "BetvilerArbeidsufoerhet")
        assertEquals(BehandlingsKategori.BESTRIDER_SYKEMELDING, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = false, gjelderUtland = false))
    }

    @Test
    fun speil_relatert() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(17000), null), BigDecimal(17000))
        assertEquals(BehandlingsKategori.SPEIL_RELATERT, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = true, gjelderUtland = false))
        assertEquals(BehandlingsKategori.SPEIL_RELATERT, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = true, gjelderUtland = true))
    }

    @Test
    fun gjelder_utland() {
        val inntektsmelding = mockInntektsmelding(Refusjon(beloepPrMnd = BigDecimal(17000), null), BigDecimal(17000))
        assertEquals(BehandlingsKategori.UTLAND, finnBehandlingsKategori(inntektsmelding = inntektsmelding, speilRelatert = false, gjelderUtland = true))
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
            endringerIRefusjon = emptyList(),
            opphørAvNaturalYtelse = emptyList(),
            gjenopptakelserNaturalYtelse = emptyList(),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "ar123",
            feriePerioder = emptyList(),
            førsteFraværsdag = LocalDate.now(),
            mottattDato = LocalDateTime.now(),
            sakId = "sak123",
            aktorId = "aktor123",
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
