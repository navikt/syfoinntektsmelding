package no.nav.syfo.repository

import no.nav.syfo.dto.InntektsmeldingEntitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
open class InntektsmeldingRepositoryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var respository: InntektsmeldingRepository

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    @Test
    fun findByAktorId(){
        val behandlet = LocalDateTime.of(2019,10,1,5,18,45,0)
        val inntektsmelding = InntektsmeldingEntitet(
            journalpostId = "journalpostId",
            behandlet = behandlet,
            sakId = "sakId",
            orgnummer = "orgnummer",
            arbeidsgiverPrivat = "arbeidsgiverPrivat",
            aktorId = "aktorId1"
        )
        inntektsmelding.leggtilArbeidsgiverperiode(fom = LocalDate.of(2019, 10, 5), tom = LocalDate.of(2019, 10, 25))
        entityManager.persist<Any>(inntektsmelding)
        val inntektsmeldinger = respository.findByAktorId("aktorId1")
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        val i = inntektsmeldinger[0]
        assertThat(i.uuid).isNotNull()
        assertThat(i.journalpostId).isEqualTo("journalpostId")
        assertThat(i.sakId).isEqualTo("sakId")
        assertThat(i.orgnummer).isEqualTo("orgnummer")
        assertThat(i.arbeidsgiverPrivat).isEqualTo("arbeidsgiverPrivat")
        assertThat(i.aktorId).isEqualTo("aktorId1")
        assertThat(i.behandlet).isEqualTo(LocalDateTime.of(2019,10,1,5,18,45,0))
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(1)
        assertThat(i.arbeidsgiverperioder[0].inntektsmelding).isEqualTo( i)
        assertThat(i.arbeidsgiverperioder[0].uuid).isNotNull()
        assertThat(i.arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019,10,5))
        assertThat(i.arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019,10,25))
    }

    @Test
    fun lagre_flere_arbeidsgiverperioder(){
        val behandlet = LocalDateTime.of(2019,10,1,5,18,45,0)
        val inntektsmelding = InntektsmeldingEntitet(
            journalpostId = "journalpostId",
            behandlet = behandlet,
            sakId = "sakId",
            orgnummer = "orgnummer",
            arbeidsgiverPrivat = "arbeidsgiverPrivat",
            aktorId = "aktorId2"
        )
        inntektsmelding.leggtilArbeidsgiverperiode(fom = LocalDate.of(2019, 10, 5), tom = LocalDate.of(2019, 10, 25))
        inntektsmelding.leggtilArbeidsgiverperiode(fom = LocalDate.of(2018, 10, 5), tom = LocalDate.of(2018, 10, 25))
        entityManager.persist<Any>(inntektsmelding)
        val inntektsmeldinger = respository.findByAktorId("aktorId2")
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        val i = inntektsmeldinger[0]
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(2)
        assertThat(i.arbeidsgiverperioder[0].inntektsmelding).isEqualTo( i)
        assertThat(i.arbeidsgiverperioder[0].uuid).isNotNull()
        assertThat(i.arbeidsgiverperioder[0].fom).isEqualTo( LocalDate.of(2019,10,5))
        assertThat(i.arbeidsgiverperioder[0].tom).isEqualTo( LocalDate.of(2019,10,25))
    }

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
        val inntektsmeldinger = respository.findByAktorId("aktorId3")
        val i = inntektsmeldinger[0]
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(0)
    }

}
