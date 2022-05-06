package no.nav.syfo.domain.inntektsmelding

import no.nav.syfo.repository.buildIM
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class InntektsmeldingTest {

    val im1 = buildIM().copy(bruttoUtbetalt = BigDecimal(100))
    val im2 = buildIM().copy(bruttoUtbetalt = BigDecimal(200))
    val im3 = buildIM().copy(bruttoUtbetalt = BigDecimal(100))
    val im4 = buildIM().copy(bruttoUtbetalt = BigDecimal(300), arsakTilInnsending = "Tull")
    val im5 = buildIM().copy(bruttoUtbetalt = BigDecimal(300), arsakTilInnsending = "Tall")

    @Test
    fun `Skal være like`() {
        kotlin.test.assertEquals(im1, im3)
        kotlin.test.assertTrue(im1.isDuplicate(im3))
    }

    @Test
    fun `Skal være ulike`() {
        kotlin.test.assertNotEquals(im1, im2)
        kotlin.test.assertFalse(im1.isDuplicate(im2))
    }

    @Test
    fun `Skal finne riktig index`() {
        kotlin.test.assertEquals(2, im1.indexOf(listOf(im4, im5, im3)))
    }

    @Test
    fun `Skal ikke finne riktig index`() {
        kotlin.test.assertEquals(-1, im3.indexOf(listOf(im4, im5)))
    }
}
