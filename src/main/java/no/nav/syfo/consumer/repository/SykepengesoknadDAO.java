package no.nav.syfo.consumer.repository;

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

    public List<Sykepengesoknad> finnSykepengesoknad(String aktoerId, String orgnummer) {

        return namedParameterJdbcTemplate.query(
                "SELECT * FROM SYKEPENGESOEKNAD " +
                        "INNER JOIN SYKMELDING_DOK ON SYKEPENGESOEKNAD.SYKMELDING_DOK_ID = SYKMELDING_DOK.SYKMELDING_DOK_ID " +
                        "WHERE ORGNUMMER = :orgnummer AND SYKMELDING_DOK.AKTOR_ID = :aktoerId",

                new MapSqlParameterSource()
                        .addValue("aktoerId", aktoerId)
                        .addValue("orgnummer", orgnummer),
                (rs, i) -> new Sykepengesoknad(rs.getString("SYKEPENGESOEKNAD_UUID"), rs.getString("STATUS")));
    }
}
