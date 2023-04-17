package no.nav.syfo.repository

import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

interface InntektsmeldingRepository {
    fun findByJournalpost(journalpostId: String): InntektsmeldingEntitet?
    fun findByUuid(uuid: String): InntektsmeldingEntitet
    fun findByArkivReferanse(arkivRefereanse: String): InntektsmeldingEntitet
    fun findByAktorId(aktoerId: String): List<InntektsmeldingEntitet>
    fun findFirst100ByBehandletBefore(førDato: LocalDateTime): List<InntektsmeldingEntitet>
    fun deleteByBehandletBefore(førDato: LocalDateTime): Int
    fun lagreInnteksmelding(innteksmelding: InntektsmeldingEntitet): InntektsmeldingEntitet
    fun deleteAll()
    fun findAll(): List<InntektsmeldingEntitet>
}

class InntektsmeldingRepositoryMock : InntektsmeldingRepository {
    private val mockrepo = mutableSetOf<InntektsmeldingEntitet>()
    override fun findByAktorId(aktoerId: String): List<InntektsmeldingEntitet> {
        return mockrepo.filter { it.aktorId == aktoerId }
    }

    override fun findFirst100ByBehandletBefore(førDato: LocalDateTime): List<InntektsmeldingEntitet> {
        return mockrepo.filter { it.behandlet!!.isBefore(førDato) }.take(100)
    }

    override fun deleteByBehandletBefore(førDato: LocalDateTime): Int {
        mockrepo.filter { it.behandlet!!.isBefore(førDato) }
            .forEach { mockrepo.remove(it) }
        return (mockrepo.size)
    }

    override fun lagreInnteksmelding(innteksmelding: InntektsmeldingEntitet): InntektsmeldingEntitet {
        mockrepo.add(innteksmelding)
        return innteksmelding
    }

    override fun deleteAll() {}

    override fun findAll(): List<InntektsmeldingEntitet> {
        return mockrepo.toList()
    }

    override fun findByJournalpost(journalpostId: String): InntektsmeldingEntitet? {
        TODO("Not yet implemented")
    }

    override fun findByUuid(uuid: String): InntektsmeldingEntitet {
        TODO("Not yet implemented")
    }

    override fun findByArkivReferanse(arkivRefereanse: String): InntektsmeldingEntitet {
        TODO("Not yet implemented")
    }
}

class InntektsmeldingRepositoryImp(
    private val ds: DataSource
) : InntektsmeldingRepository {
    private val agpRepo = ArbeidsgiverperiodeRepositoryImp(ds)
    override fun findByJournalpost(journalpostId: String): InntektsmeldingEntitet? {
        val findByAktorId = "SELECT * FROM INNTEKTSMELDING WHERE JOURNALPOST_ID = ?;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val result: InntektsmeldingEntitet?
        ds.connection.use {
            val res = it.prepareStatement(findByAktorId).apply {
                setString(1, journalpostId)
            }.executeQuery()
            result = resultLoop(res, inntektsmeldinger).firstOrNull()
        }
        return result
    }

    override fun findByUuid(uuid: String): InntektsmeldingEntitet {
        val findByAktorId = "SELECT * FROM INNTEKTSMELDING WHERE INNTEKTSMELDING_UUID = ?;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val result: InntektsmeldingEntitet
        ds.connection.use {
            val res = it.prepareStatement(findByAktorId).apply {
                setString(1, uuid)
            }.executeQuery()
            result = resultLoop(res, inntektsmeldinger).first()
        }
        result.arbeidsgiverperioder = finnAgpForIm(uuid).toMutableList()
        return result
    }

    override fun findByArkivReferanse(arkivRefereanse: String): InntektsmeldingEntitet {
        val sqlQuery = "SELECT * FROM INNTEKTSMELDING WHERE data ->> 'arkivRefereranse' = ?;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val result: InntektsmeldingEntitet
        ds.connection.use {
            val res = it.prepareStatement(sqlQuery).apply {
                setString(1, arkivRefereanse)
            }.executeQuery()
            result = resultLoop(res, inntektsmeldinger).first()
        }
        result.arbeidsgiverperioder = finnAgpForIm(result.uuid).toMutableList()
        return result
    }

    override fun findByAktorId(aktoerId: String): List<InntektsmeldingEntitet> {
        val findByAktorId = "SELECT * FROM INNTEKTSMELDING WHERE AKTOR_ID = ?;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val results: ArrayList<InntektsmeldingEntitet>
        ds.connection.use {
            val res = it.prepareStatement(findByAktorId).apply {
                setString(1, aktoerId.toString())
            }.executeQuery()
            results = resultLoop(res, inntektsmeldinger)
        }
        return addArbeidsgiverperioderTilInnteksmelding(results)
    }

    override fun findFirst100ByBehandletBefore(førDato: LocalDateTime): ArrayList<InntektsmeldingEntitet> {
        val findFirst100ByBehandletBefore = " SELECT * FROM INNTEKTSMELDING WHERE BEHANDLET < ? LIMIT 100;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val results: ArrayList<InntektsmeldingEntitet>
        ds.connection.use {
            val prepareStatement = it.prepareStatement(findFirst100ByBehandletBefore)
            prepareStatement.setTimestamp(1, Timestamp.valueOf(førDato))
            val res = prepareStatement.executeQuery()
            results = resultLoop(res, inntektsmeldinger)
        }

        return addArbeidsgiverperioderTilInnteksmelding(results)
    }

    override fun deleteByBehandletBefore(førDato: LocalDateTime): Int {
        val deleteFirst100ByBehandletBefore = "DELETE FROM INNTEKTSMELDING WHERE BEHANDLET < ?;"
        ds.connection.use {
            val prepareStatement = it.prepareStatement(deleteFirst100ByBehandletBefore)
            prepareStatement.setTimestamp(1, Timestamp.valueOf(førDato))
            return prepareStatement.executeUpdate()
        }
    }

    override fun lagreInnteksmelding(innteksmelding: InntektsmeldingEntitet): InntektsmeldingEntitet {
        val insertStatement =
            """INSERT INTO INNTEKTSMELDING (INNTEKTSMELDING_UUID, AKTOR_ID, ORGNUMMER, JOURNALPOST_ID, BEHANDLET, ARBEIDSGIVER_PRIVAT, DATA)
        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
        RETURNING *;
            """.trimMargin()
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        var result: InntektsmeldingEntitet
        ds.connection.use {
            val ps = it.prepareStatement(insertStatement)
            ps.setString(1, innteksmelding.uuid)
            ps.setString(2, innteksmelding.aktorId)
            ps.setString(3, innteksmelding.orgnummer)
            ps.setString(4, innteksmelding.journalpostId)
            ps.setTimestamp(5, Timestamp.valueOf(innteksmelding.behandlet))
            ps.setString(6, innteksmelding.arbeidsgiverPrivat)
            ps.setString(7, innteksmelding.data)

            val res = ps.executeQuery()
            result = resultLoop(res, inntektsmeldinger).first()
            lagreArbeidsgiverperioder(innteksmelding.arbeidsgiverperioder, it)
        }
        result.arbeidsgiverperioder = finnAgpForIm(result.uuid).toMutableList()
        return result
    }

    private fun finnAgpForIm(imUuid: String): List<ArbeidsgiverperiodeEntitet> {
        return agpRepo.find(imUuid)
    }

    private fun addArbeidsgiverperioderTilInnteksmelding(results: ArrayList<InntektsmeldingEntitet>): ArrayList<InntektsmeldingEntitet> {
        results.forEach { inntek ->
            val aperioder = finnAgpForIm(inntek.uuid)
            inntek.arbeidsgiverperioder = aperioder.filter { it.inntektsmelding_uuid == inntek.uuid }.toMutableList()
        }
        return results
    }

    private fun lagreArbeidsgiverperioder(arbeidsgiverperioder: List<ArbeidsgiverperiodeEntitet>, connection: Connection) {
        val rep = agpRepo
        rep.lagreDataer(arbeidsgiverperioder, connection)
    }

    override fun deleteAll() {
        val deleteStatememnt = "DELETE FROM INNTEKTSMELDING;"
        ds.connection.use {
            it.prepareStatement(deleteStatememnt).executeUpdate()
        }
    }

    override fun findAll(): List<InntektsmeldingEntitet> {
        val findall = " SELECT * FROM INNTEKTSMELDING;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        ds.connection.use {
            val res = it.prepareStatement(findall).executeQuery()
            return resultLoop(res, inntektsmeldinger)
        }
    }

    private fun resultLoop(
        res: ResultSet,
        returnValue: ArrayList<InntektsmeldingEntitet>
    ): ArrayList<InntektsmeldingEntitet> {
        while (res.next()) {
            returnValue.add(
                InntektsmeldingEntitet(
                    uuid = res.getString("INNTEKTSMELDING_UUID"),
                    aktorId = res.getString("AKTOR_ID"),
                    orgnummer = res.getString("ORGNUMMER"),
                    journalpostId = res.getString("JOURNALPOST_ID"),
                    behandlet = res.getTimestamp("BEHANDLET").toLocalDateTime(),
                    arbeidsgiverPrivat = res.getString("ARBEIDSGIVER_PRIVAT"),
                    data = res.getString("data")
                )
            )
        }

        return returnValue
    }
}
