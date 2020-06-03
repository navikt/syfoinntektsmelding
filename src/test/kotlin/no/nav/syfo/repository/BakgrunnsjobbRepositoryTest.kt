package no.nav.syfo.repository

import junit.framework.Assert.assertEquals
import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.dto.BakgrunnsjobbStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime.now
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Transactional
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = arrayOf((AutoConfigureTestDatabase::class)))
open class BakgrunnsjobbRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: BakgrunnsjobbRepository

    @Before
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `finner en opprettet jobb til utførelse`() {
        repository.saveAndFlush(
            BakgrunnsjobbEntitet(
                kjoeretid = now().minusMinutes(1),
                type = "test",
                data = """{"test-data": 1337}"""
            )
        )

        val jobber =
            repository.findByKjoeretidBeforeAndStatusIn(now(), setOf(BakgrunnsjobbStatus.OPPRETTET))

        assertEquals(1, jobber.size)
    }
}
