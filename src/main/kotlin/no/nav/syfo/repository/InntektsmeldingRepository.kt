package no.nav.syfo.repository

import no.nav.syfo.dto.InntektsmeldingEntitet
import java.sql.ResultSet

import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource


interface InntektsmeldingRepository  {
    fun findByAktorId(aktoerId: String): List<InntektsmeldingEntitet>
    fun findFirst100ByBehandletBefore(førDato: LocalDateTime): List<InntektsmeldingEntitet>
    fun deleteByBehandletBefore(førDato: LocalDateTime): Long
    fun lagreInnteksmelding(innteksmelding : InntektsmeldingEntitet): InntektsmeldingEntitet
}


class InntektsmeldingRepositoryImp(
    val ds: DataSource
) : InntektsmeldingRepository {

    override fun findByAktorId(id: String): List<InntektsmeldingEntitet> {
        val findByAktorId = "SELECT * FROM INNTEKTSMELDING WHERE AKTOR_ID = ?;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(findByAktorId).apply {
                setString(1, id.toString())
            }.executeQuery()
            return resultLoop(res, inntektsmeldinger)
        }
    }

    override fun findFirst100ByBehandletBefore(førDato: LocalDateTime): ArrayList<InntektsmeldingEntitet> {
        val findFirst100ByBehandletBefore = " SELECT * FROM INNTEKTSMELDING WHERE BEHANDLET < $førDato LIMIT = 100;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(findFirst100ByBehandletBefore).executeQuery()
            return resultLoop(res, inntektsmeldinger)
        }
    }

    override fun deleteByBehandletBefore(førDato: LocalDateTime): Long {
        val deleteFirst100ByBehandletBefore = "DELETE FROM INNTEKTSMELDING WHERE BEHANDLET < $førDato;"
        ds.connection.use {
            return  it.prepareStatement(deleteFirst100ByBehandletBefore).executeUpdate() as Long
        }
    }

    override fun lagreInnteksmelding(innteksmelding : InntektsmeldingEntitet): InntektsmeldingEntitet {
    val insertStatement = """INSERT INTO INNTEKTSMELDING (INNTEKTSMELDING_UUID, AKTOR_ID, ORGNUMMER, SAK_ID, JOURNALPOST_ID, BEHANDLET, ARBEIDSGIVER_PRIVAT, DATA)
        VALUES (${innteksmelding.uuid}, ${innteksmelding.aktorId}, ${innteksmelding.orgnummer}, ${innteksmelding.sakId}, ${innteksmelding.journalpostId}, ${innteksmelding.behandlet}, ${innteksmelding.arbeidsgiverPrivat}, ${innteksmelding.data})
        RETURNING *;""".trimMargin()
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(insertStatement).executeQuery()
            return resultLoop(res, inntektsmeldinger).first()
        }
    }

    private fun resultLoop(res : ResultSet, returnValue :ArrayList<InntektsmeldingEntitet>): ArrayList<InntektsmeldingEntitet> {
        while(res.next()) {
            returnValue.add(InntektsmeldingEntitet(
                uuid = res.getString("INNTEKTSMELDING_UUID"),
                aktorId = res.getString("AKTOR_ID"),
                orgnummer = res.getString("ORGNUMMER"),
                sakId = res.getString("SAK_ID"),
                journalpostId = res.getString("JOURNALPOST_ID"),
                behandlet = LocalDateTime.parse(res.getString("BEHANDLET")),
                arbeidsgiverPrivat = res.getString("ARBEIDSGIVER_PRIVAT"),
                data = res.getString("DATA")))
        }

        return returnValue
    }
}
