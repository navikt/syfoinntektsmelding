package no.nav.syfo.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Sykepengesoknad;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@Transactional
@Repository
public class SykepengesoknadDAO {
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public SykepengesoknadDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Optional<Sykepengesoknad> finnSykepengesoknad(String journalpostId) {
        Optional<Sykepengesoknad> resultat = Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(
                "SELECT * FROM SYKEPENGESOEKNAD WHERE JOURNALPOST_ID = :journalpostId",
                new MapSqlParameterSource().addValue("journalpostId", journalpostId),
                (rs, i) -> Sykepengesoknad.builder()
                        .uuid(rs.getString("SYKEPENGESOEKNAD_UUID"))
                        .status(rs.getString("STATUS"))
                        .oppgaveId(rs.getString("OPPGAVE_ID"))
                        .saksId(rs.getString("SAKS_ID"))
                        .journalpostId(journalpostId)
                        .build()
        ));

        if (resultat.isPresent()) {
            log.info("Fant ingen sykepengesøknad med journalpost: {}", journalpostId);
        } else {
            log.info("Fant sykepengesøknad med journalpost: {}", journalpostId);
        }

        return resultat;
    }
}