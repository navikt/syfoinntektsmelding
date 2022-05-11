package no.nav.helsearbeidsgiver.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.grunnleggendeInntektsmelding
import no.nav.syfo.repository.createTestHikariConfig
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

internal class DbTest {

    val ds = HikariDataSource(createTestHikariConfig())
    val mapper: Mapper = TODO()

    @Test
    fun findBySql() {
        val db = Db(HikariDataSource(createTestHikariConfig()), mapper)
        val inntekt = Inntekt("qwe", "", grunnleggendeInntektsmelding)
        db.add(inntekt)
        db.find("12312-5af-315zqwr", Inntekt::class.java)
        db.update(inntekt)
        db.remove(inntekt)
        val inntekter: List<Inntekt> = db.find(Query(Inntekt::class.java)
            .eq("/arbeidsgiverPerioder/fom", LocalDate.now())
            .gt("ARKIVREFERANSE", "123")
            .sortAsc("ARKIVREFERANSE")
            .limit(15))

    }

    @Test
    fun count() {
    }

    @Test
    fun find() {
    }

    @Test
    fun testFind() {
    }

    @Test
    fun add() {
    }

    @Test
    fun update() {
    }

    @Test
    fun remove() {
    }

    @Test
    fun getDs() {
    }

    @Test
    fun getMapper() {
    }
}
