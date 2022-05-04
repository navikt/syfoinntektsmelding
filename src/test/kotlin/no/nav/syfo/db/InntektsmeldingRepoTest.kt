package no.nav.syfo.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.repository.InntektsmeldingRepositoryImp
import no.nav.syfo.repository.createTestHikariConfig
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class InntektsmeldingRepoTest {

    @Test
    fun list() {
        //repo.add("AktørID", "SakID", "JournalpostID", "Orgnummer", "ArbeidsgiverPrivat")
        assertTrue(buildRepo().findAll().size > 0)
    }

    fun buildRepo(): InntektsmeldingRepositoryImp {
        val ds = HikariDataSource(createTestHikariConfig())
        val om = ObjectMapper()
        return InntektsmeldingRepositoryImp(ds, om)

    }
}
