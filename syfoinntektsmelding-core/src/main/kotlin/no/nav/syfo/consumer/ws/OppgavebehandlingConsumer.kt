package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.Oppgave
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.OpprettOppgave
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.OpprettOppgaveRequest
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDate.now
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import java.time.ZoneId
import java.util.GregorianCalendar

@Component
class OppgavebehandlingConsumer(private val oppgavebehandlingV3: OppgavebehandlingV3) {

    var log = log()

    fun opprettOppgave(fnr: String, oppgave: Oppgave) {
        try {
            val o = OpprettOppgave()
            o.brukerId = fnr
            o.brukertypeKode = "PERSON"
            o.oppgavetypeKode = "INNT_SYK"
            o.fagomradeKode = "SYK"
            o.underkategoriKode = "SYK_SYK"
            o.prioritetKode = "NORM_SYK"
            o.beskrivelse = oppgave.beskrivelse
            o.dokumentId = oppgave.dokumentId
            o.mottattDato = DatatypeFactory.newInstance().newXMLGregorianCalendar()
            o.aktivFra = date(now())
            o.aktivTil = oppgave.aktivTil?.let { date(it) }
            o.ansvarligEnhetId = oppgave.ansvarligEnhetId
            o.dokumentId = oppgave.dokumentId
            o.mottattDato = date(now())
            o.saksnummer = oppgave.saksnummer
            o.oppfolging = "\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" + "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding"

            val request = OpprettOppgaveRequest()
            request.opprettOppgave = o
            request.opprettetAvEnhetId = 9999

            oppgavebehandlingV3.opprettOppgave(request).oppgaveId
        } catch (e: RuntimeException) {
            log.error("Klarte ikke å opprette oppgave. ", e)
            throw RuntimeException(e)
        }
    }

    fun date(localdate : LocalDate) : XMLGregorianCalendar {
        val date = localdate
        val gcal = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()))
        val xcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
        return xcal
    }
}
