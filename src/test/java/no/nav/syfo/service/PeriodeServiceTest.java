package no.nav.syfo.service;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext
public class PeriodeServiceTest {

    @Mock
    private SykepengesoknadDAO sykepengesoknadDAO;

    @InjectMocks
    private PeriodeService periodeService;


    @Test
    public void erSendtInnSoknadForPeriode() {
        when(sykepengesoknadDAO.finnSykepengesoknad("journalpostid-1")).thenReturn(Optional.of(Sykepengesoknad.builder().status("APEN").journalpostId("journalpostid-1").oppgaveId("oppgaveid-1").saksId("saksid-1").uuid("uuid-1").build()));
        when(sykepengesoknadDAO.finnSykepengesoknad("journalpostid-2")).thenReturn(Optional.of(Sykepengesoknad.builder().status("LUKKET").journalpostId("journalpostid-2").oppgaveId("oppgaveid-2").saksId("saksid-2").uuid("uuid-2").build()));

        assertThat(periodeService.erSendtInnSoknadForPeriode("journalpostid-1")).isTrue();
        assertThat(periodeService.erSendtInnSoknadForPeriode("journalpostid-2")).isFalse();
    }


}