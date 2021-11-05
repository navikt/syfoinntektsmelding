package no.nav.syfo.repository

import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource
import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet

interface ArbeidsgiverperiodeRepository {
    fun lagreData(agp: ArbeidsgiverperiodeEntitet, connection: Connection): List<ArbeidsgiverperiodeEntitet>
    fun deleteAll()
}

class ArbeidsgiverperiodeRepositoryImp(private val ds: DataSource) : ArbeidsgiverperiodeRepository {

    override fun lagreData(agp: ArbeidsgiverperiodeEntitet, connection: Connection): List<ArbeidsgiverperiodeEntitet> {
        val insertStatement =
            """INSERT INTO ARBEIDSGIVERPERIODE (PERIODE_UUID, INNTEKTSMELDING_UUID, FOM, TOM)
        VALUES (?, ?, ?::date, ?::date)
        RETURNING *;""".trimMargin()
        val arbeidsgiverperioder = ArrayList<ArbeidsgiverperiodeEntitet>()
        val prepareStatement = connection.prepareStatement(insertStatement)
        prepareStatement.setString(1, "${agp.uuid}")
        prepareStatement.setString(2, "${agp.inntektsmelding?.uuid}")
        prepareStatement.setString(3, "${agp.fom}")
        prepareStatement.setString(4, "${agp.tom}")
        val res = prepareStatement.executeQuery()
        return resultLoop(res, arbeidsgiverperioder)
    }

    fun lagreDataer(arbeidsgiverperiodeEntiteter: List<ArbeidsgiverperiodeEntitet>, connection: Connection) {
        arbeidsgiverperiodeEntiteter.forEach { agiver ->
            lagreData(agiver, connection)
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

    fun find(imUuid: String): List<ArbeidsgiverperiodeEntitet> {
        val find = " SELECT * FROM ARBEIDSGIVERPERIODE where inntektsmelding_uuid = ?;"
        val arbeidsperioder = ArrayList<ArbeidsgiverperiodeEntitet>()
        ds.connection.use {
            val prepareStatement = it.prepareStatement(find)
            prepareStatement.setString(1, imUuid)
            val res = prepareStatement.executeQuery()
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
                    inntektsmelding_uuid = res.getString("INNTEKTSMELDING_UUID")
                )
            )
        }
        return returnValue
    }
}
