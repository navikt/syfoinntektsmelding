package no.nav.syfo.db

import org.jetbrains.exposed.sql.Table

object Inntektsmeldinger : Table("inntektsmelding") {
    val uuid = varchar("INNTEKTSMELDING_UUID", length = 100).uniqueIndex()
    val aktorId = varchar("AKTOR_ID", length = 50)
    val sakId = varchar("SAK_ID", length = 50)
    val journalpostId = varchar("JOURNALPOST_ID", length = 50)
    val orgnummer = varchar("ORGNUMMER", length = 50).nullable()
    val arbeidsgiverPrivat = varchar("ARBEIDSGIVER_PRIVAT", length = 50).nullable()
    val behandlet = datetime("BEHANDLET")
    val data = varchar("DATA", length = 50)
}
