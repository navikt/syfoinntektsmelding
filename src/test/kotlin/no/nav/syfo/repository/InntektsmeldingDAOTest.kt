package no.nav.syfo.repository


import no.nav.syfo.LocalApplication
import no.nav.syfo.domain.InntektsmeldingMeta
import no.nav.syfo.domain.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.util.Arrays.asList
import javax.inject.Inject

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@TestPropertySource(locations = ["classpath:application-test.properties"])
@DirtiesContext
class InntektsmeldingDAOTest {

    @Inject
    private lateinit var inntektsmeldingDAO: InntektsmeldingDAO

    @Inject
    private lateinit var jdbcTemplate: JdbcTemplate

    @Before
    fun setup() {
        jdbcTemplate.update("DELETE FROM INNTEKTSMELDING")
        jdbcTemplate.update("DELETE FROM ARBEIDSGIVERPERIODE")
    }

    @Test
    fun henterAlleInntektsmeldingenePaaAktorOgOrgnummer() {
        val melding1 = InntektsmeldingMeta(
            aktorId = "aktorId-1",
            orgnummer = "orgnummer-1",
            journalpostId = "journalpostId",
            sakId = "1",
            arbeidsgiverperioder = asList(
                Periode(
                    fom = LocalDate.of(2019, 1, 3),
                    tom = LocalDate.of(2019, 1, 19)
                )
            )
        )

        val melding2 = InntektsmeldingMeta(
            aktorId = "aktorId-1",
            orgnummer = "orgnummer-2",
            sakId = "2",
            journalpostId = "journalpostId",
            arbeidsgiverperioder = asList(
                Periode(
                    fom = LocalDate.of(2019, 1, 3),
                    tom = LocalDate.of(2019, 1, 19)
                )
            )
        )

        val melding3 = InntektsmeldingMeta(
            aktorId = "aktorId-2",
            orgnummer = "orgnummer-1",
            sakId = "3",
            journalpostId = "journalpostId",
            arbeidsgiverperioder = asList(
                Periode(
                    fom = LocalDate.of(2019, 1, 3),
                    tom = LocalDate.of(2019, 1, 19)
                )
            )
        )

        val melding4 = InntektsmeldingMeta(
            aktorId = "aktorId-2",
            orgnummer = "orgnummer-2",
            sakId = "4",
            journalpostId = "journalpostId",
            arbeidsgiverperioder = asList(
                Periode(
                    fom = LocalDate.of(2019, 1, 3),
                    tom = LocalDate.of(2019, 1, 19)
                )
            )
        )

        inntektsmeldingDAO.opprett(melding1)
        inntektsmeldingDAO.opprett(melding2)
        inntektsmeldingDAO.opprett(melding3)
        inntektsmeldingDAO.opprett(melding4)

        val inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId-1")

        assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("1")
    }

    @Test
    fun lagrerInntektsmeldingMedArbeidsgiverPrivatperson() {
        val meta = InntektsmeldingMeta(
            aktorId = "aktor",
            sakId = "saksId",
            journalpostId = "journalpostId",
            behandlet = LocalDate.of(2019, 2, 6).atStartOfDay(),
            orgnummer = null,
            arbeidsgiverPrivat = "fnrprivat"
        )

        inntektsmeldingDAO.opprett(meta)

        val metas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktor")
        assertThat(metas[0].arbeidsgiverPrivat).isEqualTo("fnrprivat")
        assertThat(metas[0].orgnummer).isEqualTo(null)
    }
}
