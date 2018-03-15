package no.nav.syfo.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Sykmelding;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SykmeldingDAO {
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public SykmeldingDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Sykmelding finnSykmelding(String id) {
        return namedParameterJdbcTemplate.queryForObject(
                "SELECT MELDING_ID, STATUS FROM SYKMELDING_DOK WHERE MELDING_ID = :id",
                new MapSqlParameterSource("id", id),
                (rs, i) -> new Sykmelding(rs.getString("melding_id"), rs.getString("status")));
    }
}
