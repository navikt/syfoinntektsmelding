package no.nav.syfo.mask

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MaskingKtTest {

    @Test
    @DisplayName("Skal skjule alt utenom siste")
    fun maskNumber1() {
        assertEquals("xxx456", "123456".maskLast(3))
    }

    @Test
    @DisplayName("Skal håndtere for få antall bokstaver")
    fun maskNumber2() {
        assertEquals("56", "56".maskLast(5))
    }

    @Test
    @DisplayName("Skal håndtere tom string")
    fun maskNumber3() {
        assertEquals("", "".maskLast(3))
    }
}
