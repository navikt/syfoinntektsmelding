package no.nav.syfo.repository

import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

interface InntektsmeldingRepository {
    fun findByUuid(uuid: String): InntektsmeldingEntitet
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
        return mockrepo.filter { it.behandlet!!.isBefore(førDato)  }.take(100)
    }

    override fun deleteByBehandletBefore(førDato: LocalDateTime): Int {
        mockrepo.filter { it.behandlet!!.isBefore(førDato) }
            .forEach { mockrepo.remove(it)}
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

    override fun findByUuid(uuid: String): InntektsmeldingEntitet {
        TODO("Not yet implemented")
    }
}

class InntektsmeldingRepositoryImp(
    private val ds: DataSource
) : InntektsmeldingRepository {
    override fun findByUuid(uuid: String): InntektsmeldingEntitet {
        val findByAktorId = "SELECT * FROM INNTEKTSMELDING WHERE INNTEKTSMELDING_UUID = ?;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val result : InntektsmeldingEntitet
        ds.connection.use {
            val res = it.prepareStatement(findByAktorId).apply {
                setString(1, uuid)
            }.executeQuery()
            result = resultLoop(res, inntektsmeldinger).first()
        }
        result.arbeidsgiverperioder = findAllArbeidsgiverperioder().filter { it.inntektsmelding_uuid == result.uuid }.toMutableList()
        return result
    }

    override fun findByAktorId(id: String): List<InntektsmeldingEntitet> {
        val findByAktorId = "SELECT * FROM INNTEKTSMELDING WHERE AKTOR_ID = ?;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val results : ArrayList<InntektsmeldingEntitet>
        ds.connection.use {
            val res = it.prepareStatement(findByAktorId).apply {
                setString(1, id.toString())
            }.executeQuery()
            results = resultLoop(res, inntektsmeldinger)
        }
        return addArbeidsgiverperioderTilInnteksmelding(results)
    }

    override fun findFirst100ByBehandletBefore(førDato: LocalDateTime): ArrayList<InntektsmeldingEntitet> {
        val findFirst100ByBehandletBefore = " SELECT * FROM INNTEKTSMELDING WHERE BEHANDLET < '${Timestamp.valueOf(førDato)}' LIMIT 100;"
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        val results : ArrayList<InntektsmeldingEntitet>
        ds.connection.use {
            val res = it.prepareStatement(findFirst100ByBehandletBefore).executeQuery()
            results = resultLoop(res, inntektsmeldinger)
        }

        return addArbeidsgiverperioderTilInnteksmelding(results)
    }

    override fun deleteByBehandletBefore(førDato: LocalDateTime): Int {
        val deleteFirst100ByBehandletBefore = "DELETE FROM INNTEKTSMELDING WHERE BEHANDLET < '${Timestamp.valueOf(førDato)}';"
        ds.connection.use {
            return it.prepareStatement(deleteFirst100ByBehandletBefore).executeUpdate()
        }
    }

    override fun lagreInnteksmelding(innteksmelding: InntektsmeldingEntitet): InntektsmeldingEntitet {
        val insertStatement =
            """INSERT INTO INNTEKTSMELDING (INNTEKTSMELDING_UUID, AKTOR_ID, ORGNUMMER, SAK_ID, JOURNALPOST_ID, BEHANDLET, ARBEIDSGIVER_PRIVAT, DATA)
        VALUES ('${innteksmelding.uuid}', '${innteksmelding.aktorId}', '${innteksmelding.orgnummer}', '${innteksmelding.sakId}', '${innteksmelding.journalpostId}', '${Timestamp.valueOf(innteksmelding.behandlet)}', '${innteksmelding.arbeidsgiverPrivat}','${innteksmelding.data}')
        RETURNING *;""".trimMargin()
        val inntektsmeldinger = ArrayList<InntektsmeldingEntitet>()
        var result : InntektsmeldingEntitet
        ds.connection.use {
            val res = it.prepareStatement(insertStatement).executeQuery()
            result = resultLoop(res, inntektsmeldinger).first()
        }
        lagreArbeidsgiverperioder(innteksmelding.arbeidsgiverperioder, innteksmelding.uuid)
        result.arbeidsgiverperioder = findAllArbeidsgiverperioder().filter { it.inntektsmelding_uuid == result.uuid }.toMutableList()
        return result
    }

    private fun addArbeidsgiverperioderTilInnteksmelding(results : ArrayList<InntektsmeldingEntitet>) : ArrayList<InntektsmeldingEntitet>{
        val aperioder = findAllArbeidsgiverperioder()
        results.forEach{ inntek ->
            inntek.arbeidsgiverperioder = aperioder.filter { it.inntektsmelding_uuid == inntek.uuid }.toMutableList()
        }
        return results
    }

    private fun findAllArbeidsgiverperioder(): List<ArbeidsgiverperiodeEntitet> {
        val rep = ArbeidsgiverperiodeRepositoryImp(ds, null)
        return rep.findAll()
    }

    private fun lagreArbeidsgiverperioder(arbeidsgiverperioder: List<ArbeidsgiverperiodeEntitet>, uuid : String) {
        val rep = ArbeidsgiverperiodeRepositoryImp(ds, null)
        rep.lagreDataer(arbeidsgiverperioder, uuid)
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
                    sakId = res.getString("SAK_ID"),
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

