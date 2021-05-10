package no.nav.syfo.repository

import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource


interface UtsattOppgaveRepository {
    fun findByInntektsmeldingId(inntektsmeldingId: String): UtsattOppgaveEntitet?
    fun findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(
        timeout: LocalDateTime,
        tilstand: Tilstand
    ): List<UtsattOppgaveEntitet>

    fun lagreInnteksmelding(innteksmelding: UtsattOppgaveEntitet): UtsattOppgaveEntitet
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

    override fun lagreInnteksmelding(innteksmelding: UtsattOppgaveEntitet): UtsattOppgaveEntitet {
        mockrepo.add(innteksmelding)
        return innteksmelding
    }

    override fun deleteAll() {
        mockrepo.forEach { mockrepo.remove(it)}
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
        val queryString = " SELECT * FROM UTSATT_OPPGAVE WHERE TIMEOUT < '${Timestamp.valueOf(timeout)}' AND TILSTAND = '${tilstand.name}';"
        val utsattoppgaver = ArrayList<UtsattOppgaveEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(queryString).executeQuery()
            return resultLoop(res, utsattoppgaver)
        }
    }

    override fun lagreInnteksmelding(utsattOppgave: UtsattOppgaveEntitet): UtsattOppgaveEntitet {
        val insertStatement =
            """INSERT INTO UTSATT_OPPGAVE (OPPGAVE_ID, INNTEKTSMELDING_ID, ARKIVREFERANSE, FNR, AKTOR_ID, SAK_ID, JOURNALPOST_ID, TIMEOUT, TILSTAND)
        VALUES ('${utsattOppgave.id}', '${utsattOppgave.inntektsmeldingId}', '${utsattOppgave.arkivreferanse}', '${utsattOppgave.fnr}', '${utsattOppgave.aktørId}', '${utsattOppgave.sakId}', '${utsattOppgave.journalpostId}', '${Timestamp.valueOf(utsattOppgave.timeout)}', '${utsattOppgave.tilstand.name}')
        RETURNING *;""".trimMargin()
        val utsattOppgaver = ArrayList<UtsattOppgaveEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(insertStatement).executeQuery()
            return resultLoop(res, utsattOppgaver).first()
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
                    tilstand = Tilstand.valueOf(res.getString("TILSTAND"))
                )
            )
        }

        return returnValue
    }
}

