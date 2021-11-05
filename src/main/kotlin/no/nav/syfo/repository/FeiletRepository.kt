package no.nav.syfo.repository

import java.sql.ResultSet
import java.sql.Timestamp
import javax.sql.DataSource
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.dto.FeiletEntitet

interface FeiletRepository {
    fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet>
    fun lagreInnteksmelding(utsattOppgave: FeiletEntitet): FeiletEntitet
    fun deleteAll()
}

class FeiletRepositoryMock : FeiletRepository {
    private val mockrep = mutableSetOf<FeiletEntitet>()
    override fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet> {
        return mockrep.filter { it.arkivReferanse == arkivReferanse }
    }

    override fun lagreInnteksmelding(utsattOppgave: FeiletEntitet): FeiletEntitet {
        mockrep.add(utsattOppgave)
        return utsattOppgave
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

    override fun lagreInnteksmelding(feil: FeiletEntitet): FeiletEntitet {
        val insertStatement = """INSERT INTO FEILET (FEILET_ID, ARKIVREFERANSE, TIDSPUNKT, FEILTYPE)
        VALUES (?, ?, ?, ?)
        RETURNING *;""".trimMargin()
        ds.connection.use {
            val prepareStatement = it.prepareStatement(insertStatement)
            prepareStatement.setInt(1, feil.id)
            prepareStatement.setString(2, feil.arkivReferanse)
            prepareStatement.setTimestamp(3, Timestamp.valueOf(feil.tidspunkt))
            prepareStatement.setString(4, feil.feiltype.name)

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

    private fun resultLoop(res: ResultSet): ArrayList<FeiletEntitet> {
        val returnValue = ArrayList<FeiletEntitet>()
        while (res.next()) {
            returnValue.add(
                FeiletEntitet(
                    id = res.getInt("FEILET_ID"),
                    arkivReferanse = res.getString("ARKIVREFERANSE"),
                    tidspunkt = res.getTimestamp("TIDSPUNKT").toLocalDateTime(),
                    feiltype = Feiltype.valueOf(res.getString("FEILTYPE"))
                )
            )
        }

        return returnValue
    }
}

