package no.nav.syfo.repository;


import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.Periode;
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
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
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
        jdbcTemplate.update("DELETE FROM ARBEIDSGIVERPERIODE");
    }

    @Test
    public void henterAlleInntektsmeldingenePaaAktorOgOrgnummer() {
        InntektsmeldingMeta melding1 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-1")
                .orgnummer("orgnummer-1")
                .sakId("1")
                .arbeidsgiverperioder(asList(Periode.builder()
                        .fom(LocalDate.of(2019, 1, 3))
                        .tom(LocalDate.of(2019, 1, 19))
                        .build()))
                .build();

        InntektsmeldingMeta melding2 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-1")
                .orgnummer("orgnummer-2")
                .sakId("2")
                .arbeidsgiverperioder(asList(Periode.builder()
                        .fom(LocalDate.of(2019, 1, 3))
                        .tom(LocalDate.of(2019, 1, 19))
                        .build()))
                .build();

        InntektsmeldingMeta melding3 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-2")
                .orgnummer("orgnummer-1")
                .sakId("3")
                .arbeidsgiverperioder(asList(Periode.builder()
                        .fom(LocalDate.of(2019, 1, 3))
                        .tom(LocalDate.of(2019, 1, 19))
                        .build()))
                .build();

        InntektsmeldingMeta melding4 = InntektsmeldingMeta
                .builder()
                .aktorId("aktorId-2")
                .orgnummer("orgnummer-2")
                .sakId("4")
                .arbeidsgiverperioder(asList(Periode.builder()
                        .fom(LocalDate.of(2019, 1, 3))
                        .tom(LocalDate.of(2019, 1, 19))
                        .build()))
                .build();

        inntektsmeldingDAO.opprett(melding1);
        inntektsmeldingDAO.opprett(melding2);
        inntektsmeldingDAO.opprett(melding3);
        inntektsmeldingDAO.opprett(melding4);

        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId-1");

        assertThat(inntektsmeldingMetas.size()).isEqualTo(2);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo("1");
    }

    @Test
    public void lagererInntektsmeldingMedArbeidsgiverPrivatperson() {
        InntektsmeldingMeta meta = InntektsmeldingMeta.builder()
                .aktorId("aktor")
                .sakId("saksId")
                .behandlet(LocalDate.of(2019, 2, 6).atStartOfDay())
                .orgnummer(null)
                .arbeidsgiverPrivat("fnrprivat")
                .arbeidsgiverperioder(Collections.emptyList())
                .build();
        inntektsmeldingDAO.opprett(meta);

        List<InntektsmeldingMeta> metas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktor");
        assertThat(metas.get(0).getArbeidsgiverPrivat()).isEqualTo("fnrprivat");
        assertThat(metas.get(0).getOrgnummer()).isEqualTo(null);
    }
}
