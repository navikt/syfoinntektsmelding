package no.nav.syfo.repository;


import no.nav.syfo.LocalApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext
public class InntektsmeldingDAOTest {

    @Inject
    private InntektsmeldingDAO inntektsmeldingDAO;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        jdbcTemplate.update("DELETE FROM INNTEKTSMELDING");

        InntektsmeldingMeta melding1 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-1")
                .orgnummer("orgnummer-1")
                .sakId("1")
                .arbeidsgiverperiodeFom(LocalDate.of(2019, 1, 3))
                .arbeidsgiverperiodeTom(LocalDate.of(2019, 1, 19))
                .build();

        InntektsmeldingMeta melding2 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-1")
                .orgnummer("orgnummer-2")
                .sakId("2")
                .arbeidsgiverperiodeFom(LocalDate.of(2019, 1, 3))
                .arbeidsgiverperiodeTom(LocalDate.of(2019, 1, 19))
                .build();

        InntektsmeldingMeta melding3 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-2")
                .orgnummer("orgnummer-1")
                .sakId("3")
                .arbeidsgiverperiodeFom(LocalDate.of(2019, 1, 3))
                .arbeidsgiverperiodeTom(LocalDate.of(2019, 1, 19))
                .build();

        InntektsmeldingMeta melding4 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-2")
                .orgnummer("orgnummer-2")
                .sakId("4")
                .arbeidsgiverperiodeFom(LocalDate.of(2019, 1, 3))
                .arbeidsgiverperiodeTom(LocalDate.of(2019, 1, 19))
                .build();

        inntektsmeldingDAO.opprett(melding1);
        inntektsmeldingDAO.opprett(melding2);
        inntektsmeldingDAO.opprett(melding3);
        inntektsmeldingDAO.opprett(melding4);
    }

    @Test
    public void henterAlleInntektsmeldingenePaaAktorOgOrgnummer() {
        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId-1", "orgnummer-1");

        assertThat(inntektsmeldingMetas.size()).isEqualTo(1);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo("1");
    }
}
