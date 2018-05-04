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

        jdbcTemplate.update(
                "INSERT INTO SYKEPENGESOEKNAD (SYKEPENGESOEKNAD_ID, SYKEPENGESOEKNAD_UUID, OPPRETTET_DATO , STATUS, BEKREFTET_OPPLYSNINGSPLIKT, BEKREFTET_KORREKT_INFO, SAKS_ID, JOURNALPOST_ID, OPPGAVE_ID, DEL) " +
                        "VALUES (1, 'uuid-1', DATE '2018-02-01', 'status', 1, 1, 'saksid-1', 'journalpostid-1', 'oppgaveid-1', 1)");

    }

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM SYKEPENGESOEKNAD");
    }

    @Test
    public void henterUtSykepengesoknad() {
        final Sykepengesoknad sykepengesoknad = sykepengesoknadDAO.finnSykepengesoknad("journalpostid-1").get();

        assertThat(sykepengesoknad.getUuid()).isEqualTo("uuid-1");
        assertThat(sykepengesoknad.getJournalpostId()).isEqualTo("journalpostid-1");
        assertThat(sykepengesoknad.getOppgaveId()).isEqualTo("oppgaveid-1");
        assertThat(sykepengesoknad.getSaksId()).isEqualTo("saksid-1");
    }
}