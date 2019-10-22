package no.nav.syfo.domain

import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

data class InngaaendeJournal(
        val dokumentId: String,
        val status: JournalStatus,
        val mottattDato: XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar()
)

enum class JournalStatus {
    MIDLERTIDIG, ANNET, ENDELIG
}
