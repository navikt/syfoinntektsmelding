package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.behandling.FerdigstillJournalfoeringFerdigstillingIkkeMuligException
import no.nav.syfo.behandling.FerdigstillJournalfoeringJournalpostIkkeInngaaendeException
import no.nav.syfo.behandling.FerdigstillJournalfoeringObjektIkkeFunnetException
import no.nav.syfo.behandling.FerdigstillJournalfoeringSikkerhetsbegrensningException
import no.nav.syfo.behandling.FerdigstillJournalfoeringUgyldigInputException
import no.nav.syfo.behandling.OppdaterJournalpostJournalpostIkkeInngaaendeException
import no.nav.syfo.behandling.OppdaterJournalpostObjektIkkeFunnetException
import no.nav.syfo.behandling.OppdaterJournalpostOppdateringIkkeMuligException
import no.nav.syfo.behandling.OppdaterJournalpostSikkerhetsbegrensningException
import no.nav.syfo.behandling.OppdaterJournalpostUgyldigInputException
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringFerdigstillingIkkeMulig
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringJournalpostIkkeInngaaende
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringObjektIkkeFunnet
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringUgyldigInput
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostJournalpostIkkeInngaaende
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostObjektIkkeFunnet
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostOppdateringIkkeMulig
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostUgyldigInput
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.ArkivSak
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Avsender
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.OppdaterJournalpostRequest
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person
import org.springframework.stereotype.Component

@Component
class BehandleInngaaendeJournalConsumer(private val behandleInngaaendeJournalV1: BehandleInngaaendeJournalV1) {

    var log = log()

    fun oppdaterJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId
        val avsenderNr = inngaendeJournalpost.arbeidsgiverOrgnummer
                ?: inngaendeJournalpost.arbeidsgiverPrivat
                ?: throw RuntimeException("Mangler avsender")

        val person = Person()
        person.ident = inngaendeJournalpost.fnr

        val avsender = Avsender()
        avsender.avsenderId = avsenderNr
        avsender.avsenderNavn = "Arbeidsgiver"

        val arkivSak = ArkivSak()
        arkivSak.arkivSakId = inngaendeJournalpost.gsakId
        arkivSak.arkivSakSystem = "FS22"

        val inn = InngaaendeJournalpost()
        inn.journalpostId = journalpostId
        inn.avsender = avsender
        inn.bruker = person
        inn.arkivSak = arkivSak

        val request = OppdaterJournalpostRequest()
        request.inngaaendeJournalpost = inn
        try {
            behandleInngaaendeJournalV1.oppdaterJournalpost( request )
        } catch (e: OppdaterJournalpostUgyldigInput) {
            log.error("Feil ved oppdatering av journalpost: {} - Ugyldig input!", journalpostId, e)
            throw OppdaterJournalpostUgyldigInputException(journalpostId, e)
        } catch (e: OppdaterJournalpostObjektIkkeFunnet) {
            log.error("Feil ved oppdatering av journalpost: {} - Journalpost ikke funnet!", journalpostId, e)
            throw OppdaterJournalpostObjektIkkeFunnetException(journalpostId, e)
        } catch (e: OppdaterJournalpostOppdateringIkkeMulig) {
            log.error("Feil ved oppdatering av journalpost: {} - Oppdatering ikke mulig!", journalpostId, e)
            throw OppdaterJournalpostOppdateringIkkeMuligException(journalpostId, e)
        } catch (e: OppdaterJournalpostJournalpostIkkeInngaaende) {
            log.error("Feil ved oppdatering av journalpost: {} - Journalpost er ikke inngående!", journalpostId, e)
            throw OppdaterJournalpostJournalpostIkkeInngaaendeException(journalpostId, e)
        } catch (e: OppdaterJournalpostSikkerhetsbegrensning) {
            log.error("Feil ved oppdatering av journalpost: {} - Sikkerhetsbegrensning!", journalpostId, e)
            throw OppdaterJournalpostSikkerhetsbegrensningException(journalpostId, e)
        }
    }

    fun ferdigstillJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId

        val request = FerdigstillJournalfoeringRequest()
        request.enhetId = inngaendeJournalpost.behandlendeEnhetId
        request.journalpostId = journalpostId

        try {
            behandleInngaaendeJournalV1.ferdigstillJournalfoering(request)
        } catch (e: FerdigstillJournalfoeringUgyldigInput) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ugyldig input!", journalpostId, e)
            throw FerdigstillJournalfoeringUgyldigInputException(journalpostId, e)
        } catch (e: FerdigstillJournalfoeringObjektIkkeFunnet) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Journalpost ikke funnet", journalpostId, e)
            throw FerdigstillJournalfoeringObjektIkkeFunnetException(journalpostId, e)
        } catch (e: FerdigstillJournalfoeringJournalpostIkkeInngaaende) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ikke inngående!", journalpostId, e)
            throw FerdigstillJournalfoeringJournalpostIkkeInngaaendeException(journalpostId, e)
        } catch (e: FerdigstillJournalfoeringSikkerhetsbegrensning) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Sikkerhetsbegrensing!", journalpostId, e)
            throw FerdigstillJournalfoeringSikkerhetsbegrensningException(journalpostId, e)
        } catch (e: FerdigstillJournalfoeringFerdigstillingIkkeMulig) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ikke mulig å ferdigstille!", journalpostId, e)
            throw FerdigstillJournalfoeringFerdigstillingIkkeMuligException(journalpostId, e)
        }

    }
}
