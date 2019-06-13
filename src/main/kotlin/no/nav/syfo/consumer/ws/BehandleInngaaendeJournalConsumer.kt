package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.BehandleInngaaendeJournalV1
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.FerdigstillJournalfoeringFerdigstillingIkkeMulig
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.FerdigstillJournalfoeringJournalpostIkkeInngaaende
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.FerdigstillJournalfoeringObjektIkkeFunnet
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.FerdigstillJournalfoeringSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.FerdigstillJournalfoeringUgyldigInput
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.OppdaterJournalpostJournalpostIkkeInngaaende
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.OppdaterJournalpostObjektIkkeFunnet
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.OppdaterJournalpostOppdateringIkkeMulig
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.OppdaterJournalpostSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.OppdaterJournalpostUgyldigInput
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.WSArkivSak
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.WSAvsender
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.WSFerdigstillJournalfoeringRequest
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.WSInngaaendeJournalpost
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.WSOppdaterJournalpostRequest
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.WSPerson
import org.springframework.stereotype.Component

@Component
class BehandleInngaaendeJournalConsumer(private val behandleInngaaendeJournalV1: BehandleInngaaendeJournalV1) {

    var log = log()

    fun oppdaterJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId
        val avsender = inngaendeJournalpost.arbeidsgiverOrgnummer
            ?: inngaendeJournalpost.arbeidsgiverPrivat
            ?: throw RuntimeException("Mangler avsender")

        val inngaaendeJournalpost = WSInngaaendeJournalpost()
            .withJournalpostId(journalpostId)
            .withBruker(WSPerson().withIdent(inngaendeJournalpost.fnr))
            .withAvsender(WSAvsender().withAvsenderNavn("Arbeidsgiver").withAvsenderId(avsender))
            .withArkivSak(WSArkivSak().withArkivSakId(inngaendeJournalpost.gsakId).withArkivSakSystem("FS22"))

        try {
            behandleInngaaendeJournalV1.oppdaterJournalpost(
                WSOppdaterJournalpostRequest()
                    .withInngaaendeJournalpost(inngaaendeJournalpost)
            )
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
        try {
            behandleInngaaendeJournalV1.ferdigstillJournalfoering(
                WSFerdigstillJournalfoeringRequest()
                    .withEnhetId(inngaendeJournalpost.behandlendeEnhetId)
                    .withJournalpostId(journalpostId)
            )
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
