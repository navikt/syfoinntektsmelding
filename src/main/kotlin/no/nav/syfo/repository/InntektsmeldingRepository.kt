package no.nav.syfo.repository

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.db.Inntektsmeldinger
import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet
import no.nav.syfo.dto.InntektsmeldingEntitet
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

interface InntektsmeldingRepository {
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

    override fun findByUuid(uuid: String): InntektsmeldingEntitet {
        TODO("Not yet implemented")
    }

    override fun findByArkivReferanse(arkivRefereanse: String): InntektsmeldingEntitet {
        TODO("Not yet implemented")
    }
}

class InntektsmeldingRepositoryImp(
    val datasource: DataSource,
    val objectMapper: ObjectMapper
) : InntektsmeldingRepository {

    fun add(aktor_id: String, sak_id: String, journalpost_id: String, orgnr: String, arbeidsgiverPriv: String): InntektsmeldingEntitet {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.insert {
                it[ uuid ] = UUID.randomUUID().toString()
                it[ aktorId ] = aktor_id
                it[ sakId ] = sak_id
                it[ journalpostId ] = journalpost_id
                it[ orgnummer ] = orgnr
                it[ arbeidsgiverPrivat ] = arbeidsgiverPriv
                it[ behandlet ] = DateTime.now()
            }.resultedValues!!.first().toEntitet()
        }
    }

    fun ResultRow.toEntitet() : InntektsmeldingEntitet {
        return InntektsmeldingEntitet(
            uuid= this[ Inntektsmeldinger.uuid],
            aktorId= this[ Inntektsmeldinger.aktorId],
            sakId= this[ Inntektsmeldinger.sakId],
            journalpostId= this[ Inntektsmeldinger.journalpostId],
            orgnummer= this[ Inntektsmeldinger.orgnummer],
            arbeidsgiverPrivat= this[ Inntektsmeldinger.arbeidsgiverPrivat],
            behandlet= LocalDateTime.now(),
            data= this[ Inntektsmeldinger.data]
        )
    }

    override fun findByUuid(uuid: String): InntektsmeldingEntitet {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.select { Inntektsmeldinger.uuid eq uuid }.mapNotNull { it.toEntitet() }.first()
        }
    }

    override fun findByArkivReferanse(arkivRefereanse: String): InntektsmeldingEntitet {
        Database.connect(datasource)
        val uuid: String? = transaction {
            exec("SELECT * FROM INNTEKTSMELDING WHERE data ->> 'arkivRefereranse' = ?" ) {
                if (it.next()) {
                    it.getString("inntektsmelding_uuid")
                }
            }
            null
        }
        if (uuid == null) {
            throw java.lang.IllegalStateException("")
        }
        return findByUuid(uuid)
    }

    override fun findByAktorId(aktoerId: String): List<InntektsmeldingEntitet> {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.select { Inntektsmeldinger.aktorId eq aktoerId }.mapNotNull { it.toEntitet() }
        }
    }

    override fun findFirst100ByBehandletBefore(førDato: LocalDateTime): List<InntektsmeldingEntitet> {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.select { Inntektsmeldinger.behandlet less førDato }.limit(100).mapNotNull { it.toEntitet() }
        }
    }

    override fun deleteByBehandletBefore(førDato: LocalDateTime): Int {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.deleteWhere { Inntektsmeldinger.behandlet less førDato }
        }
    }

    override fun lagreInnteksmelding(im: InntektsmeldingEntitet): InntektsmeldingEntitet {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.insert {
                it[ uuid ] = UUID.randomUUID().toString()
                it[ aktorId ] = im.aktorId
                it[ sakId ] = im.sakId
                it[ journalpostId ] = im.journalpostId
                it[ orgnummer ] = im.orgnummer
                it[ arbeidsgiverPrivat ] = im.arbeidsgiverPrivat
                it[ behandlet ] = DateTime.now()
            }.resultedValues!!.first().toEntitet()
        }
    }

    override fun deleteAll() {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.deleteAll()
        }
    }

    override fun findAll(): List<InntektsmeldingEntitet> {
        Database.connect(datasource)
        return transaction {
            Inntektsmeldinger.selectAll().mapNotNull { it.toEntitet() }
        }
    }
}
