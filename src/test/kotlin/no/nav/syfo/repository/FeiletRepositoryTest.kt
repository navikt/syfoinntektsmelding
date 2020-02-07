package no.nav.syfo.repository


import no.nav.syfo.LocalApplication
import no.nav.syfo.dto.InntektsmeldingEntitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@DataJpaTest
@OverrideAutoConfiguration(enabled = true)
@TestPropertySource(locations = ["classpath:application-repo.properties"])
@ContextConfiguration(classes = [LocalApplication::class])
@Ignore
open class FeiletRepositoryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var respository: FeiletRepository


    @Test
    fun lagre_uten_arbeidsgiverperioder(){
        val behandlet = LocalDateTime.of(2019,10,1,5,18,45,0)
        val inntektsmelding = InntektsmeldingEntitet(
            journalpostId = "journalpostId",
            behandlet = behandlet,
            sakId = "sakId",
            orgnummer = "orgnummer",
            arbeidsgiverPrivat = "arbeidsgiverPrivat",
            aktorId = "aktorId3"
        )
        entityManager.persist<Any>(inntektsmelding)
        val feilet = respository.findByArkivReferanse("ar-123")
        assertThat(feilet.size).isEqualTo(1)
    }

}
