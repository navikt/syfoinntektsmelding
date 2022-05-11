package no.nav.helsearbeidsgiver.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class QueryTest {

    @Test
    fun `Skal finne alle`() {
        assertEquals("select * from INNTEKTSMELDING", Query(Inntekt::class.java).toSQL())
    }

    @Test
    fun `Skal finne alle med limit`() {
        assertEquals("select * from INNTEKTSMELDING limit 15", Query(Inntekt::class.java).limit(15).toSQL())
    }

    @Test
    fun `Skal finne alle med start`() {
        assertEquals("select * from INNTEKTSMELDING offset 10 limit 15", Query(Inntekt::class.java).start(10).limit(15).toSQL())
    }

    @Test
    fun `Skal sortere`() {
        assertEquals("select * from INNTEKTSMELDING order by ARKIVREFERANSE asc", Query(Inntekt::class.java).sortAsc("ARKIVREFERANSE").toSQL())
    }

    @Test
    fun `Skal sortere finne json`() {
        assertEquals("select * from INNTEKTSMELDING where data -> 'avsenderSystem' ->> 'navn' = ?", Query(Inntekt::class.java).eq("/avsenderSystem/navn", "AltinnPortal").toSQL())
    }



}
