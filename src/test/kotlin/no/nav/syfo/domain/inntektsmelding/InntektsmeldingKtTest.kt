package no.nav.syfo.domain.inntektsmelding

import no.nav.syfo.repository.buildIM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class InntektsmeldingKtTest {

    @Test
    fun `Skal være ulike`() {
        assertNotEquals(buildIM().copy(aktorId = "abc"), buildIM().copy(aktorId = "xyz"))
    }

    @Test
    fun `Skal være like`() {
        assertEquals(buildIM().copy(aktorId = "abc"), buildIM().copy(aktorId = "abc"))
    }
}
