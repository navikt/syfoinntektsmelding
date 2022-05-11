package no.nav.helsearbeidsgiver.db

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ColumnMatchKtTest {

    @Test
    fun `Skal klare 1 nivå`() {
        assertEquals("data ->> 'avsenderSystem' = ?", ColumnMatch("/avsenderSystem", MatchType.Eq, "AltinnPortal").toSQL("data"))
    }

    @Test
    fun `Skal klare 2 nivåer`() {
        assertEquals("data -> 'avsenderSystem' ->> 'navn' = ?", ColumnMatch("/avsenderSystem/navn", MatchType.Eq, "AltinnPortal").toSQL("data"))
    }

    @Test
    fun `Skal klare 3 nivåer`() {
        assertEquals("data -> 'avsenderSystem' -> 'navn' ->> 'periode' = ?", ColumnMatch("/avsenderSystem/navn/periode", MatchType.Eq, "AltinnPortal").toSQL("data"))
    }

}
