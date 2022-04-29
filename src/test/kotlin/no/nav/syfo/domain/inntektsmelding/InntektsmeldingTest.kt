package no.nav.syfo.domain.inntektsmelding

import no.nav.syfo.repository.buildIM
import org.junit.jupiter.api.Test

internal class InntektsmeldingTest {

    val im1 = buildIM().copy(fnr = "123456789")
    val im2 = buildIM().copy(fnr = "000000000")
    val im3 = buildIM().copy(fnr = "123456789")

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
}
