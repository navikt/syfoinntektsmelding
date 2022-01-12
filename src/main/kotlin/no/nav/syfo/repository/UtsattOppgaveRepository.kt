package no.nav.syfo.repository

import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

interface UtsattOppgaveRepository {
    fun findByInntektsmeldingId(inntektsmeldingId: String): UtsattOppgaveEntitet?
    fun findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(
        timeout: LocalDateTime,
        tilstand: Tilstand
    ): List<UtsattOppgaveEntitet>

    fun opprett(innteksmelding: UtsattOppgaveEntitet): UtsattOppgaveEntitet
    fun oppdater(innteksmelding: UtsattOppgaveEntitet): UtsattOppgaveEntitet
    fun deleteAll()
    fun findAll(): List<UtsattOppgaveEntitet>
}

class UtsattOppgaveRepositoryMockk : UtsattOppgaveRepository {
    private val mockrepo = mutableSetOf<UtsattOppgaveEntitet>()

    override fun findByInntektsmeldingId(inntektsmeldingId: String): UtsattOppgaveEntitet? {
        return mockrepo.firstOrNull { it.inntektsmeldingId == inntektsmeldingId }
    }

    override fun findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(
        timeout: LocalDateTime,
        tilstand: Tilstand
    ): List<UtsattOppgaveEntitet> {
        return mockrepo.filter { it.timeout < timeout && it.tilstand == tilstand }
    }

    override fun opprett(innteksmelding: UtsattOppgaveEntitet): UtsattOppgaveEntitet {
        mockrepo.add(innteksmelding)
        return innteksmelding
    }

    override fun oppdater(innteksmelding: UtsattOppgaveEntitet): UtsattOppgaveEntitet {
        return innteksmelding
    }

    override fun deleteAll() {
        mockrepo.forEach { mockrepo.remove(it) }
    }

    override fun findAll(): List<UtsattOppgaveEntitet> {
        return mockrepo.toList()
    }
}

class UtsattOppgaveRepositoryImp(private val ds: DataSource) : UtsattOppgaveRepository {
    override fun findByInntektsmeldingId(inntektsmeldingId: String): UtsattOppgaveEntitet? {
        val findByInnteksmeldingId = "SELECT * FROM UTSATT_OPPGAVE WHERE INNTEKTSMELDING_ID = ?;"
        val inntektsmeldinger = ArrayList<UtsattOppgaveEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(findByInnteksmeldingId).apply {
                setString(1, inntektsmeldingId)
            }.executeQuery()
            return resultLoop(res, inntektsmeldinger).first()
        }
    }

    override fun findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(
        timeout: LocalDateTime,
        tilstand: Tilstand
    ): List<UtsattOppgaveEntitet> {
        val queryString = " SELECT * FROM UTSATT_OPPGAVE WHERE TIMEOUT < ? AND TILSTAND = ?;"
        val utsattoppgaver = ArrayList<UtsattOppgaveEntitet>()
        ds.connection.use {
            val prepareStatement = it.prepareStatement(queryString)
            prepareStatement.setTimestamp(1, Timestamp.valueOf(timeout))
            prepareStatement.setString(2, tilstand.name)
            val res = prepareStatement.executeQuery()
            return resultLoop(res, utsattoppgaver)
        }
    }

    override fun opprett(uo: UtsattOppgaveEntitet): UtsattOppgaveEntitet {
        val insertStatement =
            """INSERT INTO UTSATT_OPPGAVE (INNTEKTSMELDING_ID, ARKIVREFERANSE, FNR, AKTOR_ID, SAK_ID, JOURNALPOST_ID, TIMEOUT, TILSTAND, GOSYS_OPPGAVE_ID, OPPDATERT, SPEIL)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING *;""".trimMargin()

        val utsattOppgaver = ArrayList<UtsattOppgaveEntitet>()

        ds.connection.use {
            val ps = it.prepareStatement(insertStatement)
            ps.setString(1, uo.inntektsmeldingId)
            ps.setString(2, uo.arkivreferanse)
            ps.setString(3, uo.fnr)
            ps.setString(4, uo.aktørId)
            ps.setString(5, uo.sakId)
            ps.setString(6, uo.journalpostId)
            ps.setTimestamp(7, Timestamp.valueOf(uo.timeout))
            ps.setString(8, uo.tilstand.name)
            ps.setString(9, uo.gosysOppgaveId)
            ps.setTimestamp(10, Timestamp.valueOf(uo.oppdatert ?: LocalDateTime.now()))
            ps.setBoolean(11, uo.speil)
            val res = ps.executeQuery()
            return resultLoop(res, utsattOppgaver).first()
        }
    }

    override fun oppdater(uo: UtsattOppgaveEntitet): UtsattOppgaveEntitet {
        val updateStatement =
            """UPDATE UTSATT_OPPGAVE SET
                INNTEKTSMELDING_ID= ?,
                ARKIVREFERANSE =  ?,
                FNR =  ?,
                AKTOR_ID =  ?,
                SAK_ID =  ?,
                JOURNALPOST_ID =  ?,
                TIMEOUT =  ?,
                TILSTAND =  ?,
                ENHET = ?,
                GOSYS_OPPGAVE_ID = ?,
                OPPDATERT = ?,
                SPEIL = ?
            WHERE OPPGAVE_ID = ?""".trimMargin()

        ds.connection.use {
            val ps = it.prepareStatement(updateStatement)
            ps.setString(1, uo.inntektsmeldingId)
            ps.setString(2, uo.arkivreferanse)
            ps.setString(3, uo.fnr)
            ps.setString(4, uo.aktørId)
            ps.setString(5, uo.sakId)
            ps.setString(6, uo.journalpostId)
            ps.setTimestamp(7, Timestamp.valueOf(uo.timeout))
            ps.setString(8, uo.tilstand.name)
            ps.setString(9, uo.enhet)
            ps.setString(10, uo.gosysOppgaveId)
            ps.setTimestamp(11, Timestamp.valueOf(uo.oppdatert ?: LocalDateTime.now()))
            ps.setBoolean(12, uo.speil)
            ps.setInt(13, uo.id)
            ps.executeUpdate()
            return uo
        }
    }

    override fun deleteAll() {
        val deleteStatememnt = "DELETE FROM UTSATT_OPPGAVE;"
        ds.connection.use {
            it.prepareStatement(deleteStatememnt).executeUpdate()
        }
    }

    override fun findAll(): List<UtsattOppgaveEntitet> {
        val findall = " SELECT * FROM UTSATT_OPPGAVE;"
        val utsattOppgaver = ArrayList<UtsattOppgaveEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(findall).executeQuery()
            return resultLoop(res, utsattOppgaver)
        }
    }

    private fun resultLoop(
        res: ResultSet,
        returnValue: ArrayList<UtsattOppgaveEntitet>
    ): ArrayList<UtsattOppgaveEntitet> {
        while (res.next()) {
            returnValue.add(
                UtsattOppgaveEntitet(
                    id = res.getInt("OPPGAVE_ID"),
                    inntektsmeldingId = res.getString("INNTEKTSMELDING_ID"),
                    arkivreferanse = res.getString("ARKIVREFERANSE"),
                    fnr = res.getString("FNR"),
                    aktørId = res.getString("AKTOR_ID"),
                    sakId = res.getString("SAK_ID"),
                    journalpostId = res.getString("JOURNALPOST_ID"),
                    timeout = res.getTimestamp("TIMEOUT").toLocalDateTime(),
                    tilstand = Tilstand.valueOf(res.getString("TILSTAND")),
                    enhet = res.getString("ENHET"),
                    gosysOppgaveId = res.getString("GOSYS_OPPGAVE_ID"),
                    oppdatert = null,
                    speil = res.getBoolean("SPEIL")
                )
            )
        }

        return returnValue
    }
}
