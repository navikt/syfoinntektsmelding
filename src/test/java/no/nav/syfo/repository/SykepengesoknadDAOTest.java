package no.nav.syfo.repository;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.Sykepengesoknad;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext
public class SykepengesoknadDAOTest {

    @Inject
    private SykepengesoknadDAO sykepengesoknadDAO;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Before
    public void initDB() {
        cleanup();

        jdbcTemplate.update("INSERT INTO SYKMELDING_DOK VALUES (1, 'aktoer1', 'orgnummer1')");
        jdbcTemplate.update("INSERT INTO SYKEPENGESOEKNAD VALUES (1, 'uuid-1', 'NY', 'saksid-1', 'journalpostid-1', DATE '2018-03-22', DATE '2018-03-27', 1)");

    }

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM SYKEPENGESOEKNAD");
        jdbcTemplate.update("DELETE FROM SYKMELDING_DOK");
    }

    @Test
    public void henterUtSykepengesoknad() {
        final List<Sykepengesoknad> sykepengesoknads = sykepengesoknadDAO.finnSykepengesoknad("aktoer1", "orgnummer1");
        assertThat(sykepengesoknads.size()).isEqualTo(1);
        assertThat(sykepengesoknads.get(0).getUuid()).isEqualTo("uuid-1");
        assertThat(sykepengesoknads.get(0).getStatus()).isEqualTo("NY");
    }
}