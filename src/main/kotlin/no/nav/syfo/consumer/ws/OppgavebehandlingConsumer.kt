package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.Oppgave
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgave
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgaveRequest
import org.springframework.stereotype.Component
import java.time.LocalDate.now

@Component
class OppgavebehandlingConsumer(private val oppgavebehandlingV3: OppgavebehandlingV3) {

    var log = log()

    fun opprettOppgave(fnr: String, oppgave: Oppgave) {
        try {
            oppgavebehandlingV3.opprettOppgave(
                WSOpprettOppgaveRequest()
                    .withOpprettetAvEnhetId(9999)
                    .withOpprettOppgave(
                        WSOpprettOppgave()
                            .withBrukerId(fnr)
                            .withBrukertypeKode("PERSON")
                            .withOppgavetypeKode("INNT_SYK")
                            .withFagomradeKode("SYK")
                            .withUnderkategoriKode("SYK_SYK")
                            .withPrioritetKode("NORM_SYK")
                            .withBeskrivelse(oppgave.beskrivelse)
                            .withAktivFra(now())
                            .withAktivTil(oppgave.aktivTil)
                            .withAnsvarligEnhetId(oppgave.ansvarligEnhetId)
                            .withDokumentId(oppgave.dokumentId)
                            .withMottattDato(now())
                            .withSaksnummer(oppgave.saksnummer)
                            .withOppfolging("\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" + "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding")
                    )
            ).oppgaveId
        } catch (e: RuntimeException) {
            log.error("Klarte ikke å opprette oppgave. ", e)
            throw RuntimeException(e)
        }

    }
}
