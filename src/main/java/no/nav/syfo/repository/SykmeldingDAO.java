package no.nav.syfo.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Sykmelding;
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
public class SykmeldingDAO {
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public SykmeldingDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<Sykmelding> hentSykmeldingerForOrgnummer(String orgnummer, String aktorid) {
        return namedParameterJdbcTemplate.query(
                "SELECT SYKMELDING_DOK_ID FROM SYKMELDING_DOK WHERE ORGNUMMER = :orgnummer AND AKTOR_ID = :aktorid",

                new MapSqlParameterSource()
                        .addValue("orgnummer", orgnummer)
                        .addValue("aktorid", aktorid),

                (rs, i) -> Sykmelding.builder()
                        .id(rs.getInt("SYKMELDING_DOK_ID"))
                        .orgnummer(orgnummer)
                        .build()
        );
    }
}
