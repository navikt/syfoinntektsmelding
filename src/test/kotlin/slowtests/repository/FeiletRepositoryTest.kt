package slowtests.repository


import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.dto.FeiletEntitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
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
import java.time.LocalDateTime
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Transactional
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = arrayOf((AutoConfigureTestDatabase::class)))
open class FeiletRepositoryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var respository: FeiletRepository

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    val NOW = LocalDateTime.now()
    val DAYS_1 = NOW.minusDays(1)
    val DAYS_8 = NOW.minusDays(8)
    val DAYS_14 = NOW.minusDays(14)

    @Test
    fun `Skal finne alle entiteter med arkivReferansen og rette verdier`(){
        val ARKIV_REFERANSE = "ar-123"
        entityManager.persist<Any>(FeiletEntitet(
            arkivReferanse = ARKIV_REFERANSE,
            tidspunkt = DAYS_1,
            feiltype = Feiltype.AKTØR_IKKE_FUNNET
        ))
        entityManager.persist<Any>(FeiletEntitet(
            arkivReferanse = "ar-222",
            tidspunkt = DAYS_1,
            feiltype = Feiltype.JMS
        ))
        entityManager.persist<Any>(FeiletEntitet(
            arkivReferanse = "ar-555",
            tidspunkt = DAYS_1,
            feiltype = Feiltype.USPESIFISERT
        ))
        entityManager.persist<Any>(FeiletEntitet(
            arkivReferanse = ARKIV_REFERANSE,
            tidspunkt = DAYS_8,
            feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
        ))
        val liste = respository.findByArkivReferanse(ARKIV_REFERANSE)
        assertThat(liste.size).isEqualTo(2)
        assertThat(liste[0].arkivReferanse).isEqualTo(ARKIV_REFERANSE)
        assertThat(liste[0].feiltype).isEqualTo(Feiltype.AKTØR_IKKE_FUNNET)
        assertThat(liste[0].tidspunkt).isEqualTo(DAYS_1)
        assertThat(liste[1].arkivReferanse).isEqualTo(ARKIV_REFERANSE)
        assertThat(liste[1].feiltype).isEqualTo(Feiltype.BEHANDLENDE_IKKE_FUNNET)
        assertThat(liste[1].tidspunkt).isEqualTo(DAYS_8)
    }

    @Test
    fun `Skal ikke finne entitet dersom arkivReferansen ikke er lagret tidligere`(){
        val ARKIV_REFERANSE = "ar-finnes-ikke"
        entityManager.persist<Any>(FeiletEntitet(
            arkivReferanse = "ar-333",
            tidspunkt = DAYS_8,
            feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
        ))
        entityManager.persist<Any>(FeiletEntitet(
            arkivReferanse = "ar-444",
            tidspunkt = DAYS_14,
            feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
        ))
        val liste = respository.findByArkivReferanse(ARKIV_REFERANSE)
        assertThat(liste.size).isEqualTo(0)
    }


}
