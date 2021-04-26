package no.nav.syfo.repository

import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.dto.FeiletEntitet
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource


interface FeiletRepository {
    fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet>
    fun lagreInnteksmelding(utsattOppgave: FeiletEntitet): FeiletEntitet
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
}

class FeiletRepositoryImp(private val ds: DataSource) : FeiletRepository {
    override fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet> {
        val queryString = "SELECT * FROM FEILET WHERE ARKIVREFERANSE = $arkivReferanse;"
        ds.connection.use {
            val res = it.prepareStatement(queryString).executeQuery()
            return resultLoop(res)
        }
    }

    override fun lagreInnteksmelding(feil: FeiletEntitet): FeiletEntitet {
        val insertStatement = """INSERT INTO FEILET (FEILET_ID, ARKIVREFERANSE, TIDSPUNKT, FEILTYPE)
        VALUES (${feil.id}, ${feil.arkivReferanse}, ${feil.tidspunkt}, ${feil.feiltype.name})
        RETURNING *;""".trimMargin()
        ds.connection.use {
            val res = it.prepareStatement(insertStatement).executeQuery()
            return resultLoop(res).first()
        }
    }

    private fun resultLoop(res: ResultSet): ArrayList<FeiletEntitet> {
        val returnValue = ArrayList<FeiletEntitet>()
        while (res.next()) {
            returnValue.add(
                FeiletEntitet(
                    id = res.getInt("FEILET_ID"),
                    arkivReferanse = res.getString("ARKIVREFERANSE"),
                    tidspunkt = LocalDateTime.parse(res.getString("TIDSPUNKT")),
                    feiltype = Feiltype.valueOf(res.getString("FEILTYPE"))
                )
            )
        }

        return returnValue
    }
}

