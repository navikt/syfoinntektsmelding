package no.nav.syfo.repository

import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.dto.FeiletEntitet
import java.sql.ResultSet
import java.sql.Timestamp
import javax.sql.DataSource

interface FeiletRepository {
    fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet>
    fun lagreInnteksmelding(feiletEntitet: FeiletEntitet): FeiletEntitet
    fun deleteAll()
}

class FeiletRepositoryMock : FeiletRepository {
    private val mockrep = mutableSetOf<FeiletEntitet>()
    override fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet> {
        return mockrep.filter { it.arkivReferanse == arkivReferanse }
    }

    override fun lagreInnteksmelding(feiletEntitet: FeiletEntitet): FeiletEntitet {
        mockrep.add(feiletEntitet)
        return feiletEntitet
    }

    override fun deleteAll() {
    }
}

class FeiletRepositoryImp(private val ds: DataSource) : FeiletRepository {
    override fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet> {
        val queryString = "SELECT * FROM FEILET WHERE ARKIVREFERANSE = ?;"
        ds.connection.use {
            val prepareStatement = it.prepareStatement(queryString)
            prepareStatement.setString(1, arkivReferanse)
            val res = prepareStatement.executeQuery()
            return resultLoop(res)
        }
    }

    override fun lagreInnteksmelding(feiletEntitet: FeiletEntitet): FeiletEntitet {
        val insertStatement = """INSERT INTO FEILET (FEILET_ID, ARKIVREFERANSE, TIDSPUNKT, FEILTYPE)
        VALUES (?, ?, ?, ?)
        RETURNING *;""".trimMargin()
        ds.connection.use {
            val prepareStatement = it.prepareStatement(insertStatement)
            prepareStatement.setInt(1, feiletEntitet.id)
            prepareStatement.setString(2, feiletEntitet.arkivReferanse)
            prepareStatement.setTimestamp(3, Timestamp.valueOf(feiletEntitet.tidspunkt))
            prepareStatement.setString(4, feiletEntitet.feiltype.name)

            val res = prepareStatement.executeQuery()
            return resultLoop(res).first()
        }
    }

    override fun deleteAll() {
        val deleteStatement = "DELETE FROM FEILET"
        ds.connection.use {
            it.prepareStatement(deleteStatement).executeUpdate()
        }
    }

    private fun resultLoop(resultSet: ResultSet): ArrayList<FeiletEntitet> {
        val returnValue = ArrayList<FeiletEntitet>()
        while (resultSet.next()) {
            returnValue.add(
                FeiletEntitet(
                    id = resultSet.getInt("FEILET_ID"),
                    arkivReferanse = resultSet.getString("ARKIVREFERANSE"),
                    tidspunkt = resultSet.getTimestamp("TIDSPUNKT").toLocalDateTime(),
                    feiltype = Feiltype.valueOf(resultSet.getString("FEILTYPE"))
                )
            )
        }

        return returnValue
    }
}
