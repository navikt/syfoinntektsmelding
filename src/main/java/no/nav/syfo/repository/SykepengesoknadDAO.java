package no.nav.syfo.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Sykepengesoknad;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
@Repository
public class SykepengesoknadDAO {
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public SykepengesoknadDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<Sykepengesoknad> hentSykepengesoknaderForPerson(String aktorid, int sykemeldingid) {
        return namedParameterJdbcTemplate.query(
                "SELECT SYKEPENGESOEKNAD_UUID, STATUS, OPPGAVE_ID, SAKS_ID, FOM, TOM " +
                        "FROM SYKEPENGESOEKNAD " +
                        "WHERE SAKS_ID IS NOT NULL AND AKTOR_ID = :aktorid AND SYKMELDING_DOK_ID = :sykmeldingid",

                new MapSqlParameterSource()
                        .addValue("aktorid", aktorid)
                        .addValue("sykmeldingid", sykemeldingid),

                (rs, i) -> Sykepengesoknad.builder()
                        .uuid(rs.getString("SYKEPENGESOEKNAD_UUID"))
                        .status(rs.getString("STATUS"))
                        .oppgaveId(rs.getString("OPPGAVE_ID"))
                        .saksId(rs.getString("SAKS_ID"))
                        .fom(rs.getDate("FOM").toLocalDate())
                        .tom(rs.getDate("TOM").toLocalDate())
                        .build()
        );
    }
}