package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.behandling.HentJournalpostJournalpostIkkeFunneteException
import no.nav.syfo.behandling.HentJournalpostJournalpostIkkeInngaaendeException
import no.nav.syfo.behandling.HentJournalpostSikkerhetsbegrensningException
import no.nav.syfo.behandling.HentJournalpostUgyldigInputException
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.*
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest
import org.springframework.stereotype.Component

@Component
class InngaaendeJournalConsumer(private val inngaaendeJournalV1: InngaaendeJournalV1) {

    var log = log()

    fun hentDokumentId(journalpostId: String): InngaaendeJournal {
        try {
            val request = HentJournalpostRequest()
            request.journalpostId = journalpostId
            val inngaaendeJournalpost = inngaaendeJournalV1.hentJournalpost(request)
                    .inngaaendeJournalpost
            return InngaaendeJournal(
                    dokumentId = inngaaendeJournalpost.hoveddokument.dokumentId,
                    status = JournalStatus.valueOf(inngaaendeJournalpost.journaltilstand.name),
                    mottattDato = inngaaendeJournalpost.forsendelseMottatt
            )


        } catch (e: HentJournalpostSikkerhetsbegrensning) {
            log.error("Feil ved henting av journalpost: Sikkerhetsbegrensning!")
            throw HentJournalpostSikkerhetsbegrensningException(journalpostId, e)
        } catch (e: HentJournalpostJournalpostIkkeInngaaende) {
            log.error("Feil ved henting av journalpost: Journalpost er ikke inngaaende!")
            throw HentJournalpostJournalpostIkkeInngaaendeException(journalpostId, e)
        } catch (e: HentJournalpostJournalpostIkkeFunnet) {
            log.error("Feil ved henting av journalpost: Journalpost ikke funnet!")
            throw HentJournalpostJournalpostIkkeFunneteException(journalpostId, e)
        } catch (e: HentJournalpostUgyldigInput) {
            log.error("Feil ved henting av journalpost: Journalpostid ikke gyldig!")
            throw HentJournalpostUgyldigInputException(journalpostId, e)
        }

    }
}
