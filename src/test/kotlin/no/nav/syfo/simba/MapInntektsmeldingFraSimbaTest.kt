package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Avsender
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Bonus
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Naturalytelse
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Permisjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RedusertLoennIAgp
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Refusjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.RefusjonEndring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykmeldt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.til
import no.nav.helsearbeidsgiver.utils.test.date.desember
import no.nav.helsearbeidsgiver.utils.test.date.februar
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.date.mai
import no.nav.helsearbeidsgiver.utils.test.date.mars
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.syfo.simba.Avsender as MuligAvsender

class MapInntektsmeldingFraSimbaTest {
    @Test
    fun mapInntektsmeldingMedNaturalytelser() {
        val naturalytelser = Naturalytelse.Kode.entries.map { Naturalytelse(it, 1.0, LocalDate.now()) }
        val antallNaturalytelser = naturalytelser.count()
        val imd =
            lagInntektsmelding().let {
                it.copy(
                    inntekt =
                        it.inntekt?.copy(
                            naturalytelser = naturalytelser,
                        ),
                )
            }
        val mapped =
            mapInntektsmelding(
                arkivreferanse = "im1323",
                aktorId = "sdfds",
                journalpostId = "134",
                im = imd,
            )
        assertEquals(antallNaturalytelser, mapped.opphørAvNaturalYtelse.size)
        val naturalytelse = mapped.opphørAvNaturalYtelse[0]
        assertEquals(no.nav.syfo.domain.inntektsmelding.Naturalytelse.AKSJERGRUNNFONDSBEVISTILUNDERKURS, naturalytelse.naturalytelse)
    }

    @Test
    fun mapRefusjon() {
        val refusjonEndringer = listOf(RefusjonEndring(123.0, 1.desember(2025)))
        val refusjon = Refusjon(10.0, refusjonEndringer, 12.desember(2025))
        val mapped =
            mapInntektsmelding(
                arkivreferanse = "im1323",
                aktorId = "sdfds",
                journalpostId = "134",
                im = lagInntektsmelding().copy(refusjon = refusjon),
            )
        assertEquals(mapped.refusjon.opphoersdato, refusjon.sluttdato)
        assertEquals(mapped.endringerIRefusjon.size, 1)
    }

    @Test
    fun mapBegrunnelseRedusert() {
        RedusertLoennIAgp.Begrunnelse.entries.forEach { begrunnelse ->
            val im =
                lagInntektsmelding().let {
                    it.copy(
                        agp =
                            it.agp?.copy(
                                redusertLoennIAgp =
                                    RedusertLoennIAgp(
                                        beloep = 1.0,
                                        begrunnelse = begrunnelse,
                                    ),
                            ),
                    )
                }

            val mapped = mapInntektsmelding("im123", "abc", "345", im)

            assertEquals(begrunnelse.name, mapped.begrunnelseRedusert, "Feil ved mapping: $begrunnelse")
            assertEquals(1.0.toBigDecimal(), mapped.bruttoUtbetalt, "Feil ved mapping: $begrunnelse")
        }
    }

    @Test
    fun mapIngenBegrunnelseRedusert() {
        val im =
            lagInntektsmelding().let {
                it.copy(
                    agp =
                        it.agp?.copy(
                            redusertLoennIAgp = null,
                        ),
                )
            }
        val mapped = mapInntektsmelding("im1", "2", "3", im)
        assertEquals("", mapped.begrunnelseRedusert)
        assertNull(mapped.bruttoUtbetalt)
    }

    @Test
    fun mapInntektEndringAarsak() {
        val im =
            lagInntektsmelding().copy(
                inntekt =
                    Inntekt(
                        beloep = 60_000.0,
                        inntektsdato = 3.mai,
                        naturalytelser = emptyList(),
                        endringAarsaker = listOf(Bonus),
                    ),
            )
        val mapped = mapInntektsmelding("im1", "2", "3", im)
        val endringAarsak = mapped.rapportertInntekt?.endringAarsakerData?.get(0)!!
        assertEquals("Bonus", endringAarsak.aarsak)
        assertNull(endringAarsak.perioder)
        assertNull(endringAarsak.gjelderFra)
        assertNull(endringAarsak.bleKjent)
    }

    @Test
    fun mapInnsendtTidspunktFraSimba() {
        val localDateTime = LocalDateTime.of(2023, 2, 11, 14, 0)
        val innsendt = OffsetDateTime.of(localDateTime, ZoneOffset.of("+1"))
        val im = mapInntektsmelding("im1", "2", "3", lagInntektsmelding().copy(mottatt = innsendt))
        assertEquals(localDateTime, im.innsendingstidspunkt)
    }

    @Test
    fun mapVedtaksperiodeID() {
        val im = mapInntektsmelding("im1", "2", "3", lagInntektsmelding().copy(vedtaksperiodeId = null))
        assertNull(im.vedtaksperiodeId)
        val vedtaksperiodeId = UUID.randomUUID()
        val im2 = mapInntektsmelding("im1", "2", "3", lagInntektsmelding().copy(vedtaksperiodeId = vedtaksperiodeId))
        assertEquals(vedtaksperiodeId, im2.vedtaksperiodeId)
    }

    @Test
    fun mapAvsenderForSelvbestemtOgVanlig() {
        val selvbestemtIm =
            lagInntektsmelding().copy(
                type = Inntektsmelding.Type.Selvbestemt(UUID.randomUUID()),
            )
        val selvbestemtMapped = mapInntektsmelding("im1", "2", "3", selvbestemtIm)
        assertEquals(MuligAvsender.NAV_NO_SELVBESTEMT, selvbestemtMapped.avsenderSystem.navn)
        assertEquals(MuligAvsender.VERSJON, selvbestemtMapped.avsenderSystem.versjon)

        val mapped = mapInntektsmelding("im1", "2", "3", lagInntektsmelding())
        assertEquals(MuligAvsender.NAV_NO, mapped.avsenderSystem.navn)
        assertEquals(MuligAvsender.VERSJON, mapped.avsenderSystem.versjon)
    }
}

fun lagInntektsmelding(): Inntektsmelding =
    Inntektsmelding(
        id = UUID.randomUUID(),
        type =
            Inntektsmelding.Type.Forespurt(
                id = UUID.randomUUID(),
            ),
        vedtaksperiodeId = UUID.randomUUID(),
        sykmeldt =
            Sykmeldt(
                fnr = Fnr.genererGyldig(),
                navn = "Syk Sykesen",
            ),
        avsender =
            Avsender(
                orgnr = Orgnr.genererGyldig(),
                orgNavn = "Blåbærsyltetøy A/S",
                navn = "Hå Erresen",
                tlf = "22555555",
            ),
        sykmeldingsperioder =
            listOf(
                10.januar til 31.januar,
                10.februar til 28.februar,
            ),
        agp =
            Arbeidsgiverperiode(
                perioder =
                    listOf(
                        1.januar til 3.januar,
                        5.januar til 5.januar,
                        10.januar til 21.januar,
                    ),
                egenmeldinger =
                    listOf(
                        1.januar til 3.januar,
                        5.januar til 5.januar,
                    ),
                redusertLoennIAgp =
                    RedusertLoennIAgp(
                        beloep = 55_555.0,
                        begrunnelse = RedusertLoennIAgp.Begrunnelse.Permittering,
                    ),
            ),
        inntekt =
            Inntekt(
                beloep = 66_666.0,
                inntektsdato = 10.januar,
                naturalytelser =
                    listOf(
                        Naturalytelse(
                            naturalytelse = Naturalytelse.Kode.BIL,
                            verdiBeloep = 123.0,
                            sluttdato = 1.februar,
                        ),
                        Naturalytelse(
                            naturalytelse = Naturalytelse.Kode.FRITRANSPORT,
                            verdiBeloep = 456.0,
                            sluttdato = 15.februar,
                        ),
                    ),
                endringAarsaker =
                    listOf(
                        Permisjon(
                            permisjoner =
                                listOf(
                                    6.januar til 6.januar,
                                    8.januar til 8.januar,
                                ),
                        ),
                    ),
            ),
        refusjon =
            Refusjon(
                beloepPerMaaned = 22_222.0,
                endringer =
                    listOf(
                        RefusjonEndring(
                            beloep = 22_111.0,
                            startdato = 1.februar,
                        ),
                        RefusjonEndring(
                            beloep = 22_000.0,
                            startdato = 2.februar,
                        ),
                    ),
                sluttdato = 25.februar,
            ),
        aarsakInnsending = AarsakInnsending.Ny,
        mottatt = 1.mars.atStartOfDay().atOffset(ZoneOffset.ofHours(1)),
    )
