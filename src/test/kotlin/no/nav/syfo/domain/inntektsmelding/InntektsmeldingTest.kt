package no.nav.syfo.domain.inntektsmelding

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.repository.buildIM
import no.nav.syfo.simba.Avsender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InntektsmeldingTest {
    val im1 = buildIM().copy(bruttoUtbetalt = BigDecimal(100))
    val im2 = buildIM().copy(bruttoUtbetalt = BigDecimal(200))
    val im3 = buildIM().copy(bruttoUtbetalt = BigDecimal(100))

    @Test
    fun `Skal være like`() {
        assertEquals(im1, im3)
        assertTrue(im1.isDuplicate(im3))
    }

    @Test
    fun `Skal være ulike`() {
        assertNotEquals(im1, im2)
        assertFalse(im1.isDuplicate(im2))
    }

    @Test
    fun `Skal være duplikat selv om arsakInnsending er ulik`() {
        val imNy = im1.copy(arsakTilInnsending = "Ny")
        val imEndring = im1.copy(arsakTilInnsending = "Endring")
        assertTrue(imNy.isDuplicateExclusiveArsakInnsending(imEndring))
        assertFalse(imNy.isDuplicate(imEndring)) // Disse skal bli ulike siden vi tar hensyn til årsakInnsending
    }

    @Test
    fun `Hvilke felter som skal ignoreres`() {
        assertTrue(im1.copy(id = "asd").isDuplicate(im1))
        assertTrue(im1.copy(fnr = "asd").isDuplicate(im1))
        assertFalse(im1.copy(arbeidsgiverOrgnummer = "asd").isDuplicate(im1))
        assertFalse(im1.copy(arbeidsgiverPrivatFnr = "asd").isDuplicate(im1))
        assertFalse(im1.copy(arbeidsgiverPrivatAktørId = "asd").isDuplicate(im1))
        assertFalse(im1.copy(arbeidsforholdId = "asd").isDuplicate(im1))
        assertTrue(im1.copy(journalpostId = "asd").isDuplicate(im1))
        assertFalse(im1.copy(arsakTilInnsending = "asd").isDuplicate(im1))
        assertTrue(im1.copy(journalStatus = JournalStatus.MOTTATT).isDuplicate(im1))
        assertFalse(im1.copy(arbeidsgiverperioder = listOf(Periode(fom = LocalDate.now(), tom = LocalDate.now()))).isDuplicate(im1))
        assertFalse(im1.copy(beregnetInntekt = BigDecimal(200)).isDuplicate(im1))
        assertFalse(im1.copy(refusjon = Refusjon(BigDecimal(123), LocalDate.now())).isDuplicate(im1))
        assertFalse(
            im1
                .copy(
                    endringerIRefusjon = listOf(EndringIRefusjon(endringsdato = LocalDate.now(), beloep = BigDecimal(123))),
                ).isDuplicate(im1),
        )
        assertFalse(
            im1
                .copy(
                    opphørAvNaturalYtelse = listOf(OpphoerAvNaturalytelse(Naturalytelse.BIL, LocalDate.now(), beloepPrMnd = BigDecimal(123))),
                ).isDuplicate(im1),
        )
        assertFalse(im1.copy(gjenopptakelserNaturalYtelse = listOf(GjenopptakelseNaturalytelse(Naturalytelse.BOLIG))).isDuplicate(im1))
        assertTrue(im1.copy(gyldighetsStatus = Gyldighetsstatus.GYLDIG).isDuplicate(im1))
        assertTrue(im1.copy(arkivRefereranse = "asd").isDuplicate(im1))
        assertFalse(im1.copy(feriePerioder = listOf(Periode(LocalDate.now(), LocalDate.now()))).isDuplicate(im1))
        assertFalse(im1.copy(førsteFraværsdag = LocalDate.now()).isDuplicate(im1))
        assertTrue(im1.copy(mottattDato = LocalDateTime.now()).isDuplicate(im1))
        assertTrue(im1.copy(sakId = "asd").isDuplicate(im1))
        assertTrue(im1.copy(aktorId = "asd").isDuplicate(im1))
        assertFalse(im1.copy(begrunnelseRedusert = "asd").isDuplicate(im1))
        assertTrue(im1.copy(avsenderSystem = AvsenderSystem("asd")).isDuplicate(im1))
        assertFalse(im1.copy(nærRelasjon = true).isDuplicate(im1))
        assertFalse(im1.copy(kontaktinformasjon = Kontaktinformasjon("asd", "asd")).isDuplicate(im1))
        assertTrue(im1.copy(innsendingstidspunkt = LocalDateTime.now()).isDuplicate(im1))
        assertFalse(im1.copy(bruttoUtbetalt = BigDecimal(123)).isDuplicate(im1))
        assertFalse(im1.copy(årsakEndring = "qwe").isDuplicate(im1))
    }

    @Test
    fun `selvbestemt matcher spleis bare hvis vedtaksperiodeId er satt`() {
        val selvbestemtUtenVedtaksperiodeId =
            im1.copy(
                vedtaksperiodeId = null,
                avsenderSystem = AvsenderSystem(Avsender.NAV_NO_SELVBESTEMT),
            )
        val selvbestemtMedVedtaksperiodeId =
            im1.copy(
                vedtaksperiodeId = UUID.randomUUID(),
                avsenderSystem = AvsenderSystem(Avsender.NAV_NO_SELVBESTEMT),
            )
        assertFalse(selvbestemtUtenVedtaksperiodeId.matcherSpleis())
        assertTrue(selvbestemtMedVedtaksperiodeId.matcherSpleis())
    }

    @Test
    fun `Alle avsendere bortsett fra selvbestemt matcher alltid spleis som default`() {
        val imFraSimbaUtenVedtaksperiodeId = im1.copy(vedtaksperiodeId = null, avsenderSystem = AvsenderSystem(Avsender.NAV_NO))
        val imFraSimbaMedVedtaksperiodeId = im1.copy(vedtaksperiodeId = UUID.randomUUID(), avsenderSystem = AvsenderSystem(Avsender.NAV_NO))
        val imFraEksterntSystemUtenVedtaksperiodeId =
            im1.copy(
                vedtaksperiodeId = null,
                avsenderSystem = AvsenderSystem("Whatever Payment Co"),
            )
        val imFraEksterntSystemMedVedtaksperiodeId =
            im1.copy(
                vedtaksperiodeId = UUID.randomUUID(),
                avsenderSystem = AvsenderSystem("Donald Duck & Co"),
            )
        val selvbestemtUtenVedtaksperiodeId =
            im1.copy(
                vedtaksperiodeId = null,
                avsenderSystem = AvsenderSystem(Avsender.NAV_NO_SELVBESTEMT),
            )
        assertTrue(imFraSimbaUtenVedtaksperiodeId.matcherSpleis())
        assertTrue(imFraSimbaMedVedtaksperiodeId.matcherSpleis())
        assertTrue(imFraEksterntSystemUtenVedtaksperiodeId.matcherSpleis())
        assertTrue(imFraEksterntSystemMedVedtaksperiodeId.matcherSpleis())
        assertFalse(selvbestemtUtenVedtaksperiodeId.matcherSpleis())
    }
}
