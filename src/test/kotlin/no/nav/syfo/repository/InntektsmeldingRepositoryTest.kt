package no.nav.syfo.repository

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import no.nav.syfo.dto.InntektsmeldingEntitet
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@ActiveProfiles("test")
open class InntektsmeldingRepositoryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var respository: InntektsmeldingRepository

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
        assertEquals(inntektsmeldinger.size,1)
        val i = inntektsmeldinger[0]
        assertNotNull(i.uuid)
        assertEquals(i.journalpostId,"journalpostId")
        assertEquals(i.sakId,"sakId")
        assertEquals(i.orgnummer,"orgnummer")
        assertEquals(i.arbeidsgiverPrivat,"arbeidsgiverPrivat")
        assertEquals(i.aktorId,"aktorId1")
        assertEquals(i.behandlet,LocalDateTime.of(2019,10,1,5,18,45,0))
        assertEquals(i.arbeidsgiverperioder.size,1)
        assertEquals(i.arbeidsgiverperioder[0].inntektsmelding, i)
        assertNotNull(i.arbeidsgiverperioder[0].uuid)
        assertEquals(i.arbeidsgiverperioder[0].fom, LocalDate.of(2019,10,5))
        assertEquals(i.arbeidsgiverperioder[0].tom, LocalDate.of(2019,10,25))
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
        assertEquals(inntektsmeldinger.size,1)
        val i = inntektsmeldinger[0]
        assertEquals(i.arbeidsgiverperioder.size,2)
        assertEquals(i.arbeidsgiverperioder[0].inntektsmelding, i)
        assertNotNull(i.arbeidsgiverperioder[0].uuid)
        assertEquals(i.arbeidsgiverperioder[0].fom, LocalDate.of(2019,10,5))
        assertEquals(i.arbeidsgiverperioder[0].tom, LocalDate.of(2019,10,25))
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
        assertEquals(inntektsmeldinger.size,1)
        assertEquals(i.arbeidsgiverperioder.size,0)
    }

}
