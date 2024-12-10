package no.nav.syfo.mapping

import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.BegrunnelseIngenEllerRedusertUtbetalingKode
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.RapportertInntekt
import no.nav.syfo.domain.inntektsmelding.SpinnInntektEndringAarsak
import no.nav.syfo.grunnleggendeInntektsmelding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class InntektsmeldingMapperFraInternSyfoTilHAGKontraktTest {
    @Test
    fun mapInntektsmeldingKontraktUtenBegrunnelseOgBeløp() {
        val syfoInternInntektsmelding = grunnleggendeInntektsmelding
        val inntektsmelding =
            mapInntektsmeldingKontrakt(
                syfoInternInntektsmelding,
                "123",
                Gyldighetsstatus.GYLDIG,
                "arkivref-123",
                UUID.randomUUID().toString(),
            )
        assertNull(inntektsmelding.bruttoUtbetalt)
        assertEquals("", inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt)
    }

    @Test
    fun mapInntektsmeldingKontrakt() {
        val bruttoUtbetalt = BigDecimal(39013)
        val begrunnelse = BegrunnelseIngenEllerRedusertUtbetalingKode.FerieEllerAvspasering.name
        val innsenderNavn = "André Bjørke"
        val innsenderTelefon = "22555555"
        val syfoInternInntektsmelding =
            grunnleggendeInntektsmelding.copy(
                bruttoUtbetalt = bruttoUtbetalt,
                begrunnelseRedusert = begrunnelse,
                kontaktinformasjon = Kontaktinformasjon(navn = innsenderNavn, telefon = innsenderTelefon),
                avsenderSystem = no.nav.syfo.domain.inntektsmelding.AvsenderSystem("NAV_NO", "1.0"),
                rapportertInntekt =
                    RapportertInntekt(
                        bekreftet = true,
                        beregnetInntekt = 39013.0,
                        endringAarsak = "Ferie",
                        endringAarsakData =
                            SpinnInntektEndringAarsak(
                                aarsak = "Ferie",
                                perioder = listOf(Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))),
                            ),
                        manueltKorrigert = true,
                    ),
            )
        val inntektsmelding =
            mapInntektsmeldingKontrakt(
                syfoInternInntektsmelding,
                "123",
                Gyldighetsstatus.GYLDIG,
                "arkivref-123",
                UUID.randomUUID().toString(),
            )
        assertEquals(bruttoUtbetalt, inntektsmelding.bruttoUtbetalt)
        assertEquals(begrunnelse, inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt)
        assertEquals(innsenderNavn, inntektsmelding.innsenderFulltNavn)
        assertEquals(innsenderTelefon, inntektsmelding.innsenderTelefon)
        assertEquals("Ferie", inntektsmelding.inntektEndringAarsak?.aarsak)
        assertEquals(1, inntektsmelding.inntektEndringAarsak?.perioder?.size)
        assertNull(inntektsmelding.inntektEndringAarsak?.gjelderFra)
        assertNull(inntektsmelding.inntektEndringAarsak?.bleKjent)
        assertTrue(inntektsmelding.matcherSpleis)
    }

    @Test
    fun mapInntektsmeldingKontraktMedOgUtenVedtaksperiodeId() {
        val inntektsmelding = grunnleggendeInntektsmelding
        val kontraktIM =
            mapInntektsmeldingKontrakt(inntektsmelding, "123", Gyldighetsstatus.GYLDIG, "arkivref123", UUID.randomUUID().toString())
        assertNull(kontraktIM.vedtaksperiodeId)

        val vedtaksperiodeId = UUID.randomUUID()
        val inntektsmeldingMedVedtaksperiodeID = inntektsmelding.copy(vedtaksperiodeId = vedtaksperiodeId)
        val kontraktIMMedVedtaksperiodeId =
            mapInntektsmeldingKontrakt(
                inntektsmeldingMedVedtaksperiodeID,
                "123",
                Gyldighetsstatus.GYLDIG,
                "arkivref123",
                UUID.randomUUID().toString(),
            )
        assertEquals(vedtaksperiodeId, kontraktIMMedVedtaksperiodeId.vedtaksperiodeId)
    }

    @Test
    fun mapInntektsmeldingKontraktSelvbestemtMatcherIkkeSpleis() {
        val inntektsmelding = grunnleggendeInntektsmelding
        val kontraktIM =
            mapInntektsmeldingKontrakt(inntektsmelding, "123", Gyldighetsstatus.GYLDIG, "arkivref123", UUID.randomUUID().toString(), false)
        assertFalse(kontraktIM.matcherSpleis)
    }

    @Test
    fun mapInntektsmeldingKontraktMatcherSpleisSomDefault() {
        val inntektsmelding = grunnleggendeInntektsmelding
        val kontraktIM =
            mapInntektsmeldingKontrakt(inntektsmelding, "123", Gyldighetsstatus.GYLDIG, "arkivref123", UUID.randomUUID().toString())
        assertTrue(kontraktIM.matcherSpleis)
    }
}
