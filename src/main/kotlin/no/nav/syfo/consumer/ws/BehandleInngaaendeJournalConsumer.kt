package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.*
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.ArkivSak
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Avsender
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.OppdaterJournalpostRequest
import org.springframework.stereotype.Component

@Component
class BehandleInngaaendeJournalConsumer(private val behandleInngaaendeJournalV1: BehandleInngaaendeJournalV1) {

    var log = log()

    fun oppdaterJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId
        val avsenderNr = inngaendeJournalpost.arbeidsgiverOrgnummer
            ?: inngaendeJournalpost.arbeidsgiverPrivat
            ?: throw RuntimeException("Mangler avsender")

        val person = no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person()
        person.ident = inngaendeJournalpost.fnr

        val avsender = Avsender()
        avsender.avsenderId = avsenderNr

        val arkivSak = ArkivSak()
        arkivSak.arkivSakId = "FS22"

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
            throw RuntimeException("Feil ved oppdatering av journalpost: $journalpostId - Ugyldig input!", e)
        } catch (e: OppdaterJournalpostObjektIkkeFunnet) {
            log.error("Feil ved oppdatering av journalpost: {} - Journalpost ikke funnet!", journalpostId, e)
            throw RuntimeException("Feil ved oppdatering av journalpost: $journalpostId Journalpost ikke funnet!", e)
        } catch (e: OppdaterJournalpostOppdateringIkkeMulig) {
            log.error("Feil ved oppdatering av journalpost: {} - Oppdatering ikke mulig!", journalpostId, e)
            throw RuntimeException("Feil ved oppdatering av journalpost: $journalpostId - Oppdatering ikke mulig!", e)
        } catch (e: OppdaterJournalpostJournalpostIkkeInngaaende) {
            log.error("Feil ved oppdatering av journalpost: {} - Journalpost er ikke inngående!", journalpostId, e)
            throw RuntimeException(
                "Feil ved oppdatering av journalpost: $journalpostId - Journalpost er ikke inngående!",
                e
            )
        } catch (e: OppdaterJournalpostSikkerhetsbegrensning) {
            log.error("Feil ved oppdatering av journalpost: {} - Sikkerhetsbegrensning!", journalpostId, e)
            throw RuntimeException("Feil ved oppdatering av journalpost: $journalpostId - Sikkerhetsbegrensning!", e)
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
            throw RuntimeException("Feil ved ferdigstilling av journalpost: $journalpostId - Ugyldig innput!", e)
        } catch (e: FerdigstillJournalfoeringObjektIkkeFunnet) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Journalpost ikke funnet", journalpostId, e)
            throw RuntimeException(
                "Feil ved ferdigstilling av journalpost: $journalpostId - Journalpost ikke funnet!",
                e
            )
        } catch (e: FerdigstillJournalfoeringJournalpostIkkeInngaaende) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ikke inngående!", journalpostId, e)
            throw RuntimeException("Feil ved ferdigstilling av journalpost: $journalpostId - Ikke inngående!", e)
        } catch (e: FerdigstillJournalfoeringSikkerhetsbegrensning) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Sikkerhetsbegrensing!", journalpostId, e)
            throw RuntimeException("Feil ved ferdigstilling av journalpost: $journalpostId - Sikkerhetsbegrensing!", e)
        } catch (e: FerdigstillJournalfoeringFerdigstillingIkkeMulig) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ikke mulig å ferdigstille!", journalpostId, e)
            throw RuntimeException(
                "Feil ved ferdigstilling av journalpost: $journalpostId - Ikke mulig å ferdigstille!",
                e
            )
        }

    }
}
