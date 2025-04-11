package no.nav.syfo.mapping

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RedusertLoennIAgp
import no.nav.inntektsmeldingkontrakt.ArsakTilInnsending
import no.nav.inntektsmeldingkontrakt.Format
import no.nav.inntektsmeldingkontrakt.MottaksKanal
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.RapportertInntekt
import no.nav.syfo.domain.inntektsmelding.SpinnInntektEndringAarsak
import no.nav.syfo.grunnleggendeInntektsmelding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class InntektsmeldingMapperFraInternSyfoTilHAGKontraktTest {
    @Test
    fun mapInntektsmeldingKontraktUtenBegrunnelseOgBeløp() {
        val syfoInternInntektsmelding = grunnleggendeInntektsmelding
        val inntektsmelding =
            mapImKontrakt(
                syfoInternInntektsmelding,
            )
        assertNull(inntektsmelding.bruttoUtbetalt)
        assertEquals("", inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt)
    }

    @Test
    fun mapInntektsmeldingKontrakt() {
        val bruttoUtbetalt = BigDecimal(39013)
        val begrunnelse = RedusertLoennIAgp.Begrunnelse.FerieEllerAvspasering.name
        val innsenderNavn = "André Bjørke"
        val innsenderTelefon = "22555555"
        val inntektEndringAarsak =
            SpinnInntektEndringAarsak(
                aarsak = "Ferie",
                perioder = listOf(Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))),
            )
        val syfoInternInntektsmelding =
            grunnleggendeInntektsmelding.copy(
                bruttoUtbetalt = bruttoUtbetalt,
                begrunnelseRedusert = begrunnelse,
                kontaktinformasjon = Kontaktinformasjon(navn = innsenderNavn, telefon = innsenderTelefon),
                avsenderSystem =
                    no.nav.syfo.domain.inntektsmelding
                        .AvsenderSystem("NAV_NO", "1.0"),
                rapportertInntekt =
                    RapportertInntekt(
                        bekreftet = true,
                        beregnetInntekt = 39013.0,
                        endringAarsakerData = listOf(inntektEndringAarsak),
                        manueltKorrigert = true,
                    ),
            )
        val inntektsmelding =
            mapImKontrakt(
                syfoInternInntektsmelding,
            )
        assertEquals(bruttoUtbetalt, inntektsmelding.bruttoUtbetalt)
        assertEquals(begrunnelse, inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt)
        assertEquals(innsenderNavn, inntektsmelding.innsenderFulltNavn)
        assertEquals(innsenderTelefon, inntektsmelding.innsenderTelefon)
        assertEquals("Ferie", inntektsmelding.inntektEndringAarsak?.aarsak)
        assertEquals(1, inntektsmelding.inntektEndringAarsak?.perioder?.size)
        assertNull(inntektsmelding.inntektEndringAarsak?.gjelderFra)
        assertNull(inntektsmelding.inntektEndringAarsak?.bleKjent)
        assertEquals(1, inntektsmelding.inntektEndringAarsaker?.size)
        assertEquals(
            inntektEndringAarsak.perioder.toString(),
            inntektsmelding.inntektEndringAarsaker
                ?.firstOrNull()
                ?.perioder
                .toString(),
        )
        assertEquals(inntektEndringAarsak.aarsak, inntektsmelding.inntektEndringAarsaker?.firstOrNull()?.aarsak)
        assertEquals(inntektEndringAarsak.gjelderFra, inntektsmelding.inntektEndringAarsaker?.firstOrNull()?.gjelderFra)
        assertEquals(inntektEndringAarsak.bleKjent, inntektsmelding.inntektEndringAarsaker?.firstOrNull()?.bleKjent)
        assertEquals(syfoInternInntektsmelding.mottaksKanal.name, inntektsmelding.mottaksKanal?.name)
    }

    @Test
    fun mapInntektsmeldingKontraktMedOgUtenVedtaksperiodeId() {
        val inntektsmelding = grunnleggendeInntektsmelding
        val kontraktIM =
            mapImKontrakt(inntektsmelding)
        assertNull(kontraktIM.vedtaksperiodeId)
        assertEquals(inntektsmelding.mottaksKanal.name, kontraktIM.mottaksKanal?.name)
        val vedtaksperiodeId = UUID.randomUUID()
        val inntektsmeldingMedVedtaksperiodeID = inntektsmelding.copy(vedtaksperiodeId = vedtaksperiodeId)
        val kontraktIMMedVedtaksperiodeId =
            mapImKontrakt(
                inntektsmeldingMedVedtaksperiodeID,
            )
        assertEquals(vedtaksperiodeId, kontraktIMMedVedtaksperiodeId.vedtaksperiodeId)
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("Ny", "ny", "TEST", "ugyldig", "", "ENDRING", "Endret"))
    fun `mapInntektsmeldingKontrakt felt arsakTilInnsending settes til defaultverdi Ny ved ugyldige verdier`(aarsak: String) {
        val inntektsmelding = grunnleggendeInntektsmelding.copy(arsakTilInnsending = aarsak)
        val kontraktIM =
            mapImKontrakt(inntektsmelding)
        assertEquals(ArsakTilInnsending.Ny, kontraktIM.arsakTilInnsending)
    }

    @Test
    fun `mapInntektsmeldingKontrakt felt arsakTilInnsending settes til Endring`() {
        val inntektsmelding = grunnleggendeInntektsmelding.copy(arsakTilInnsending = "Endring")
        val kontraktIM =
            mapImKontrakt(inntektsmelding)
        assertEquals(ArsakTilInnsending.Endring, kontraktIM.arsakTilInnsending)
    }

    @Test
    fun `mapInntektsmeldingKontrakt felt mottakskanal settes riktig`() {
        val altinnPortalInntektsmelding =
            grunnleggendeInntektsmelding.copy(
                avsenderSystem = MockData.avsenderAltinn,
                mottaksKanal = no.nav.syfo.domain.inntektsmelding.MottaksKanal.ALTINN,
            )
        val kontraktIM = mapImKontrakt(altinnPortalInntektsmelding)
        assertEquals(MottaksKanal.ALTINN, kontraktIM.mottaksKanal)
        assertEquals(Format.Inntektsmelding, kontraktIM.format)
        assertFalse(kontraktIM.forespurt)
    }

    @Test
    fun `mapInntektsmeldingKontrakt felt mottakskanal settes til nav_no`() {
        val altinnPortalInntektsmelding =
            grunnleggendeInntektsmelding.copy(
                avsenderSystem = MockData.avsenderNavPortal,
                mottaksKanal = no.nav.syfo.domain.inntektsmelding.MottaksKanal.NAV_NO,
                forespurt = true,
            )
        val kontraktIM = mapImKontrakt(altinnPortalInntektsmelding)
        assertEquals(MottaksKanal.NAV_NO, kontraktIM.mottaksKanal)
        assertEquals(Format.Arbeidsgiveropplysninger, kontraktIM.format)
        assertTrue(kontraktIM.forespurt)
    }

    @Test
    fun `mapInntektsmeldingKontrakt felt mottakskanal settes til hr_system_api`() {
        val altinnPortalInntektsmelding =
            grunnleggendeInntektsmelding.copy(
                avsenderSystem = MockData.avsenderTigerSys,
                mottaksKanal = no.nav.syfo.domain.inntektsmelding.MottaksKanal.HR_SYSTEM_API,
                forespurt = true,
            )
        val kontraktIM = mapImKontrakt(altinnPortalInntektsmelding)
        assertEquals(MottaksKanal.HR_SYSTEM_API, kontraktIM.mottaksKanal)
        assertEquals(Format.Arbeidsgiveropplysninger, kontraktIM.format)
        assertTrue(kontraktIM.forespurt)
    }
}

fun mapImKontrakt(inntektsmelding: Inntektsmelding) =
    mapInntektsmeldingKontrakt(
        inntektsmelding = inntektsmelding,
        arbeidstakerAktørId = MockData.aktørId,
        gyldighetsstatus = MockData.gyldighetsstatus,
        arkivreferanse = MockData.arkivreferanse,
        uuid = MockData.uuid,
    )

object MockData {
    val aktørId = "123"
    val gyldighetsstatus = Gyldighetsstatus.GYLDIG
    val arkivreferanse = "arkivref123"
    val uuid = UUID.randomUUID().toString()
    val avsenderAltinn = AvsenderSystem("AltinnPortal", "1.0")
    val avsenderNavPortal = AvsenderSystem("NAV_PORTAL", "1.0")
    val avsenderTigerSys = AvsenderSystem("Tigersys", "3.0")
}
