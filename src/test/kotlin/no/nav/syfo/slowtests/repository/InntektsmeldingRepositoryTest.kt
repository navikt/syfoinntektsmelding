package no.nav.syfo.slowtests.repository
/*

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.*
import no.nav.syfo.dto.InntektsmeldingEntitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Transactional
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = arrayOf((AutoConfigureTestDatabase::class)))
open class InntektsmeldingRepositoryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: InntektsmeldingRepository

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    @Before
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun findByAktorId() {
        val behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0)
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
        val inntektsmeldinger = repository.findByAktorId("aktorId1")
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        val i = inntektsmeldinger[0]
        assertThat(i.uuid).isNotNull()
        assertThat(i.journalpostId).isEqualTo("journalpostId")
        assertThat(i.sakId).isEqualTo("sakId")
        assertThat(i.orgnummer).isEqualTo("orgnummer")
        assertThat(i.arbeidsgiverPrivat).isEqualTo("arbeidsgiverPrivat")
        assertThat(i.aktorId).isEqualTo("aktorId1")
        assertThat(i.behandlet).isEqualTo(LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0))
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(1)
        assertThat(i.arbeidsgiverperioder[0].inntektsmelding).isEqualTo(i)
        assertThat(i.arbeidsgiverperioder[0].uuid).isNotNull()
        assertThat(i.arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019, 10, 5))
        assertThat(i.arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019, 10, 25))
    }

    @Test
    fun lagre_flere_arbeidsgiverperioder() {
        val behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0)
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
        val inntektsmeldinger = repository.findByAktorId("aktorId2")
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        val i = inntektsmeldinger[0]
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(2)
        assertThat(i.arbeidsgiverperioder[0].inntektsmelding).isEqualTo(i)
        assertThat(i.arbeidsgiverperioder[0].uuid).isNotNull()
        assertThat(i.arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019, 10, 5))
        assertThat(i.arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019, 10, 25))
    }

    @Test
    fun lagre_uten_arbeidsgiverperioder() {
        val behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0)
        val inntektsmelding = InntektsmeldingEntitet(
            journalpostId = "journalpostId",
            behandlet = behandlet,
            sakId = "sakId",
            orgnummer = "orgnummer",
            arbeidsgiverPrivat = "arbeidsgiverPrivat",
            aktorId = "aktorId3"
        )
        entityManager.persist<Any>(inntektsmelding)
        val inntektsmeldinger = repository.findByAktorId("aktorId3")
        val i = inntektsmeldinger[0]
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    fun lagre_inntektsmelding_som_json() {
        val im = Inntektsmelding(
            id = "id-abc",
            fnr = "fnr-123",
            arbeidsgiverOrgnummer = "arb-org-123",
            arbeidsgiverPrivatFnr = "arb-priv-123",
            arbeidsgiverPrivatAktørId = "arb-priv-aktør-123",
            arbeidsforholdId = "arb-123",
            journalpostId = "jp-123",
            arsakTilInnsending = "Ingen årsak",
            journalStatus = JournalStatus.MIDLERTIDIG,
            arbeidsgiverperioder = listOf(
                Periode(fom = LocalDate.of(2011, 11, 1), tom = LocalDate.of(2012, 12, 2)),
                Periode(fom = LocalDate.of(2013, 3, 3), tom = LocalDate.of(2014, 4, 4))
            ),
            beregnetInntekt = BigDecimal(999999999999),
            refusjon = Refusjon(BigDecimal(333333333333), LocalDate.of(2020, 2, 20)),
            endringerIRefusjon = listOf(
                EndringIRefusjon(LocalDate.of(2015, 5, 5), BigDecimal(555555555555)),
                EndringIRefusjon(LocalDate.of(2016, 6, 6), BigDecimal(666666666666))
            ),
            opphørAvNaturalYtelse = listOf(
                OpphoerAvNaturalytelse(Naturalytelse.BIL, LocalDate.of(2015, 5, 5), BigDecimal(555555555555)),
                OpphoerAvNaturalytelse(Naturalytelse.TILSKUDDBARNEHAGEPLASS, LocalDate.of(2016, 6, 6), BigDecimal(666666666666))
            ),
            gjenopptakelserNaturalYtelse = listOf(
                GjenopptakelseNaturalytelse(Naturalytelse.BOLIG, LocalDate.of(2011, 1, 1), BigDecimal(111111111111)),
                GjenopptakelseNaturalytelse(Naturalytelse.KOSTDAGER, LocalDate.of(2012, 2, 2), BigDecimal(222222222222))
            ),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "ar-123",
            feriePerioder = listOf(
                Periode(fom = LocalDate.of(2017, 7, 7), tom = LocalDate.of(2018, 8, 8)),
                Periode(fom = LocalDate.of(2019, 9, 9), tom = LocalDate.of(2020, 12, 20))
            ),

            førsteFraværsdag = LocalDate.of(2010, 2, 10),
            mottattDato = LocalDateTime.of(2010, 5, 4, 3, 2, 1),
            sakId = "sak-123",
            aktorId = "aktør-123",
            begrunnelseRedusert = "Grunn til reduksjon"
        )
        val mapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()
        val inntektsmelding = InntektsmeldingEntitet(
            journalpostId = "journalpostId",
            behandlet = LocalDateTime.now(),
            sakId = "sakId",
            orgnummer = "orgnummer",
            arbeidsgiverPrivat = "arbeidsgiverPrivat",
            aktorId = "aktorId-repo-test",
            data = mapper.writeValueAsString(im)
        )
        entityManager.persist<Any>(inntektsmelding)
        val inntektsmeldinger = repository.findByAktorId("aktorId-repo-test")
        val i = inntektsmeldinger[0]
        assertThat(inntektsmeldinger.size).isEqualTo(1)

        val data = mapper.readTree(i.data)

        assertThat(data.get("id").asText()).isEqualTo("id-abc")
        assertThat(data.get("fnr").asText()).isEqualTo("fnr-123")
        assertThat(data.get("aktorId").asText()).isEqualTo("aktør-123")
        assertThat(data.get("journalpostId").asText()).isEqualTo("jp-123")
        assertThat(data.get("refusjon").get("beloepPrMnd").asText()).isEqualTo("333333333333")
        assertThat(data.get("refusjon").get("opphoersdato").asText()).isEqualTo("2020-02-20")
        assertThat(data.get("begrunnelseRedusert").asText()).isEqualTo("Grunn til reduksjon")
        assertThat(data.get("sakId").asText()).isEqualTo("sak-123")
        assertThat(data.get("mottattDato").asText()).isEqualTo("2010-05-04T03:02:01")
        assertThat(data.get("arkivRefereranse").asText()).isEqualTo("ar-123")
        assertThat(data.get("førsteFraværsdag").asText()).isEqualTo("2010-02-10")
        assertThat(data.get("arsakTilInnsending").asText()).isEqualTo("Ingen årsak")

    }

    @Test
    fun `skal kun slette inntektsmeldinger eldre enn gitt dato`() {
        //Lagre 5 inntektsmeldinger i 2019
        val imMedPeriodeGammel = lagInntektsmelding(LocalDate.of(2019, 12, 31).atStartOfDay())
        imMedPeriodeGammel.leggtilArbeidsgiverperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))
        entityManager.persist<Any>(imMedPeriodeGammel)
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2019, 12, 30).atStartOfDay()))
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2019, 11, 25).atStartOfDay()))
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2019, 10, 14).atStartOfDay()))
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2019, 3, 15).atStartOfDay()))

        //Lagre 5 inntektsmeldinger i 2020
        val imMedPeriodeNy = lagInntektsmelding(LocalDate.of(2020, 1, 1).atStartOfDay())
        imMedPeriodeNy.leggtilArbeidsgiverperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))
        entityManager.persist<Any>(imMedPeriodeNy)
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2020, 1, 5).atStartOfDay()))
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2020, 2, 25).atStartOfDay()))
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2020, 4, 14).atStartOfDay()))
        entityManager.persist<Any>(lagInntektsmelding(LocalDate.of(2020, 5, 15).atStartOfDay()))

        //10 tilsammen
        assertThat(repository.findAll().size).isEqualTo(10)
        //5 før 2020
        assertThat(repository.findFirst100ByBehandletBefore(LocalDate.of(2020, 1, 1).atStartOfDay()).size).isEqualTo(5)

        //Slett alle før 2020
        repository.deleteByBehandletBefore(LocalDate.of(2020, 1, 1).atStartOfDay())
        //Nå skal det bare være 5 treff
        assertThat(repository.findAll().size).isEqualTo(5)

    }

    private fun lagInntektsmelding(behandlet: LocalDateTime): InntektsmeldingEntitet {
        return InntektsmeldingEntitet(
            journalpostId = "journalpostId",
            behandlet = behandlet,
            sakId = "sakId",
            orgnummer = "orgnummer",
            arbeidsgiverPrivat = "arbeidsgiverPrivat",
            aktorId = "aktorId1"
        )
    }

}
*/
