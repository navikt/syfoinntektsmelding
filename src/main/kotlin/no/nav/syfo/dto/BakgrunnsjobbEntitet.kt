package no.nav.syfo.dto

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "BAKGRUNNSJOBB")
@TypeDef(
    name = "jsonb",
    typeClass = JsonBinaryType::class
)
data class BakgrunnsjobbEntitet(

    @Id
    @Column(name = "JOBB_ID", length = 100, updatable = false)
    var uuid: String = UUID.randomUUID().toString(),

    @Column(name = "TYPE")
    var type: String,

    @Column(name = "BEHANDLET")
    var behandlet: LocalDateTime = LocalDateTime.now(),

    @Column(name = "OPPRETTET")
    var opprettet: LocalDateTime = LocalDateTime.now(),

    @Column(name = "STATUS")
    var status: BakgrunnsjobbStatus = BakgrunnsjobbStatus.OPPRETTET,

    @Column(name = "KJOERETID")
    var kjoeretid: LocalDateTime = LocalDateTime.now(),

    @Column(name = "FORSOEK")
    var forsoek: Int = 0,

    @Column(name = "MAKS_FORSOEK")
    var maksAntallForsoek: Int = 3,

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "DATA", nullable = false)
    var data: String
)

enum class BakgrunnsjobbStatus {
    /**
     * Oppgaven er opprettet og venter på kjøring
     */
    OPPRETTET,

    /**
     * Oppgaven har blitt forsøkt kjørt, men feilet. Den vil bli kjørt igjen til den når maks antall forsøk
     */
    FEILET,

    /**
     * Oppgaven ble kjørt maks antall forsøk og trenger nå manuell håndtering
     */
    STOPPET,

    /**
     * Oppgaven ble kjørt OK
     */
    OK,

    /**
     * Oppgaven er manuelt avbrutt
     */
    AVBRUTT
}
