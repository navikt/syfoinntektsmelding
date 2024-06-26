package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.BegrunnelseIngenEllerRedusertUtbetalingKode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Bonus
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.FullLoennIArbeidsgiverPerioden
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Naturalytelse
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.NaturalytelseKode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Refusjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.RefusjonEndring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class MapInntektsmeldingFraSimbaTest {
    @Test
    fun mapInntektsmeldingMedNaturalytelser() {
        val naturalytelser = NaturalytelseKode.entries.map { Naturalytelse(it, LocalDate.now(), 1.0) }
        val antallNaturalytelser = naturalytelser.count()
        val imd = lagInntektsmelding().copy(naturalytelser = naturalytelser)
        val mapped = mapInntektsmelding("1323", "sdfds", "134", imd)
        assertEquals(antallNaturalytelser, mapped.opphørAvNaturalYtelse.size)
        val naturalytelse = mapped.opphørAvNaturalYtelse.get(0)
        assertEquals(no.nav.syfo.domain.inntektsmelding.Naturalytelse.AKSJERGRUNNFONDSBEVISTILUNDERKURS, naturalytelse.naturalytelse)
    }

    @Test
    fun mapRefusjon() {
        val refusjonEndring = listOf(RefusjonEndring(123.0, LocalDate.now()))
        val refusjon = Refusjon(true, 10.0, LocalDate.of(2025, 12, 12), refusjonEndring)
        val mapped = mapInntektsmelding("1323", "sdfds", "134", lagInntektsmelding().copy(refusjon = refusjon))
        assertEquals(mapped.refusjon.opphoersdato, refusjon.refusjonOpphører)
        assertEquals(mapped.endringerIRefusjon.size, 1)
    }

    @Test
    fun mapBegrunnelseRedusert() {
        val begrunnelser = BegrunnelseIngenEllerRedusertUtbetalingKode.entries
        val mapped = begrunnelser.map { kode ->
            lagInntektsmelding().copy(
                fullLønnIArbeidsgiverPerioden = FullLoennIArbeidsgiverPerioden(
                    false,
                    kode,
                    1.0
                )
            )
        }
            .map { imd ->
                mapInntektsmelding("123", "abc", "345", imd)
            }.toList()
        for (i in mapped.indices) {
            assertEquals(begrunnelser[i].name, mapped[i].begrunnelseRedusert, "Feil ved mapping av $i: ${begrunnelser[i]}")
            assertEquals(1.0.toBigDecimal(), mapped[i].bruttoUtbetalt, "Feil ved mapping av $i: ${begrunnelser[i]}")
        }
    }

    @Test
    fun mapIngenBegrunnelseRedusert() {
        val im = mapInntektsmelding("1", "2", "3", lagInntektsmelding())
        assertEquals("", im.begrunnelseRedusert)
        assertNull(im.bruttoUtbetalt)
    }

    @Test
    fun mapInntektEndringAArsak() {
        val im = mapInntektsmelding("1", "2", "3", lagInntektsmelding().copy(inntekt = Mock.inntektEndringBonus))
        assertEquals("", im.begrunnelseRedusert)
        assertNull(im.bruttoUtbetalt)
        assertEquals("Bonus", im.rapportertInntekt?.endringAarsak)
        assertEquals("Bonus", im.rapportertInntekt?.endringAarsakData?.aarsak)
        assertNull(im.rapportertInntekt?.endringAarsakData?.perioder)
        assertNull(im.rapportertInntekt?.endringAarsakData?.gjelderFra)
        assertNull(im.rapportertInntekt?.endringAarsakData?.bleKjent)
    }

    @Test
    fun mapInnsendtTidspunktFraSimba() {
        val localDateTime = LocalDateTime.of(2023, 2, 11, 14, 0)
        val innsendt = OffsetDateTime.of(localDateTime, ZoneOffset.of("+1"))
        val im = mapInntektsmelding("1", "2", "3", lagInntektsmelding().copy(tidspunkt = innsendt))
        assertEquals(localDateTime, im.innsendingstidspunkt)
    }

    @Test
    fun mapVedtaksperiodeID() {
        val im = mapInntektsmelding("1", "2", "3", lagInntektsmelding())
        assertNull(im.vedtaksperiodeId)
        val vedtaksperiodeId = UUID.randomUUID()
        val im2 = mapInntektsmelding("1", "2", "3", lagInntektsmelding().copy(vedtaksperiodeId = vedtaksperiodeId))
        assertEquals(vedtaksperiodeId, im2.vedtaksperiodeId)
    }

    @Test
    fun mapAvsenderForSelvbestemtOgVanlig() {
        val selvbestemtIM = mapInntektsmelding("1", "2", "3", lagInntektsmelding(), selvbestemt = true)
        assertEquals(Avsender.NAV_NO_SELVBESTEMT, selvbestemtIM.avsenderSystem.navn)
        assertEquals(Avsender.VERSJON, selvbestemtIM.avsenderSystem.versjon)
        val im = mapInntektsmelding("1", "2", "3", lagInntektsmelding())
        assertEquals(Avsender.NAV_NO, im.avsenderSystem.navn)
        assertEquals(Avsender.VERSJON, im.avsenderSystem.versjon)
    }

    private fun lagInntektsmelding(): Inntektsmelding {
        val dato1 = LocalDate.now().minusDays(7)
        val dato2 = LocalDate.now().minusDays(5)
        val dato3 = LocalDate.now().minusDays(3)
        val periode1 = listOf(Periode(dato1, dato1))
        val periode2 = listOf(Periode(dato1, dato1), Periode(dato2, dato2))
        val periode3 = listOf(Periode(dato1, dato3))

        val refusjonEndring = listOf(RefusjonEndring(123.0, LocalDate.now()))
        val refusjon = Refusjon(true, 10.0, LocalDate.of(2025, 12, 12), refusjonEndring)

        val imFraSimba = Inntektsmelding(
            orgnrUnderenhet = "123456789",
            identitetsnummer = "12345678901",
            fulltNavn = "Test testesen",
            virksomhetNavn = "Blåbærsyltetøy A/S",
            behandlingsdager = listOf(dato1),
            egenmeldingsperioder = periode1,
            bestemmendeFraværsdag = dato2,
            fraværsperioder = periode2,
            arbeidsgiverperioder = periode3,
            beregnetInntekt = 100_000.0,
            refusjon = refusjon,
            naturalytelser = emptyList(),
            tidspunkt = OffsetDateTime.now(),
            årsakInnsending = AarsakInnsending.NY,
            innsenderNavn = "Peppes Pizza",
            telefonnummer = "22555555"
        )
        return imFraSimba
    }
    object Mock {
        val bonus = Bonus()
        val forslagInntekt = 50_000.0
        val endretInntekt = 60_000.0
        val inntektUtenEndring = Inntekt(
            bekreftet = true,
            beregnetInntekt = forslagInntekt,
            manueltKorrigert = false
        )
        val inntektEndringBonus = inntektUtenEndring.copy(beregnetInntekt = endretInntekt, endringÅrsak = bonus, manueltKorrigert = true)
    }
}
