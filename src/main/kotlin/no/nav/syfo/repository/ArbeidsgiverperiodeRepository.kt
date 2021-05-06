package no.nav.syfo.repository

import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.util.ArrayList
import javax.sql.DataSource

interface ArbeidsgiverperiodeRepository {
    fun lagreData(arbeidsgiverperiodeEntitet: ArbeidsgiverperiodeEntitet) : List<ArbeidsgiverperiodeEntitet>
    fun deleteAll()
}

class ArbeidsgiverperiodeRepositoryImp(private val ds: DataSource, private val inntektsmeldingRepository: InntektsmeldingRepository?) : ArbeidsgiverperiodeRepository{

    override fun lagreData(arbeidsgiverperiodeEntitet: ArbeidsgiverperiodeEntitet) : List<ArbeidsgiverperiodeEntitet>{
        val insertStatement =
            """INSERT INTO ARBEIDSGIVERPERIODE (PERIODE_UUID, INNTEKTSMELDING_UUID, FOM, TOM)
        VALUES ('${arbeidsgiverperiodeEntitet.uuid}', '${arbeidsgiverperiodeEntitet.inntektsmelding?.uuid}', '${arbeidsgiverperiodeEntitet.fom}', '${arbeidsgiverperiodeEntitet.tom}')
        RETURNING *;""".trimMargin()
        val arbeidsgiverperioder = ArrayList<ArbeidsgiverperiodeEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(insertStatement).executeQuery()
            return resultLoop(res, arbeidsgiverperioder)
        }
    }

    fun lagreDataer(arbeidsgiverperiodeEntiteter: List<ArbeidsgiverperiodeEntitet>, inntekUuid : String) {
        arbeidsgiverperiodeEntiteter.forEach { agiver ->
            val insertStatement =
                """INSERT INTO ARBEIDSGIVERPERIODE (PERIODE_UUID, INNTEKTSMELDING_UUID, FOM, TOM)
        VALUES ('${agiver.uuid}', '$inntekUuid', '${agiver.fom}', '${agiver.tom}')
        RETURNING *;""".trimMargin()
            ds.connection.use {
                it.prepareStatement(insertStatement).executeQuery()
            }
        }
    }

    override fun deleteAll() {
        val deleteStatememnt = "DELETE FROM ARBEIDSGIVERPERIODE;"
        ds.connection.use {
            it.prepareStatement(deleteStatememnt).executeUpdate()
        }
    }

    fun findAll(): List<ArbeidsgiverperiodeEntitet> {
        val findall = " SELECT * FROM ARBEIDSGIVERPERIODE;"
        val arbeidsperioder = ArrayList<ArbeidsgiverperiodeEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(findall).executeQuery()
            return resultLoop(res, arbeidsperioder)
        }
    }

    private fun resultLoop(
        res: ResultSet,
        returnValue: ArrayList<ArbeidsgiverperiodeEntitet>
    ): ArrayList<ArbeidsgiverperiodeEntitet> {
        while (res.next()) {
            returnValue.add(
                ArbeidsgiverperiodeEntitet(
                    uuid = res.getString("PERIODE_UUID"),
                    inntektsmelding = null, //inntektsmeldingRepository.findByUuid(res.getString("INNTEKTSMELDING_UUID") ),
                    fom = res.getDate("FOM").toLocalDate(),
                    tom = res.getDate("TOM").toLocalDate(),
                    inntektsmelding_uuid = res.getString("INNTEKTSMELDING_UUID")))
        }
        return returnValue
    }
}
