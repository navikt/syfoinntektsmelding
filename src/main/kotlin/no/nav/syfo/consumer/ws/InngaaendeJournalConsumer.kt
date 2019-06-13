package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.HentJournalpostJournalpostIkkeFunnet
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.HentJournalpostJournalpostIkkeInngaaende
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.HentJournalpostSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.HentJournalpostUgyldigInput
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.InngaaendeJournalV1
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.WSHentJournalpostRequest
import org.springframework.stereotype.Component

@Component
class InngaaendeJournalConsumer(private val inngaaendeJournalV1: InngaaendeJournalV1) {

    var log = log()

    fun hentDokumentId(journalpostId: String): InngaaendeJournal {
        try {
            val inngaaendeJournalpost = inngaaendeJournalV1.hentJournalpost(
                WSHentJournalpostRequest()
                    .withJournalpostId(journalpostId)
            )
                .inngaaendeJournalpost

            return InngaaendeJournal(
                dokumentId = inngaaendeJournalpost.hoveddokument.dokumentId,
                status = JournalStatus.valueOf(inngaaendeJournalpost.journaltilstand.name)
            )


        } catch (e: HentJournalpostSikkerhetsbegrensning) {
            log.error("Feil ved henting av journalpost: Sikkerhetsbegrensning!")
            throw RuntimeException("Feil ved henting av journalpost: Sikkerhetsbegrensning!", e)
        } catch (e: HentJournalpostJournalpostIkkeInngaaende) {
            log.error("Feil ved henting av journalpost: Journalpost er ikke inngaaende!")
            throw RuntimeException("Feil ved henting av journalpost: Journalpost er ikke inngaaende!", e)
        } catch (e: HentJournalpostJournalpostIkkeFunnet) {
            log.error("Feil ved henting av journalpost: Journalpost ikke funnet!")
            throw RuntimeException("Feil ved henting av journalpost: Journalpost ikke funnet!", e)
        } catch (e: HentJournalpostUgyldigInput) {
            log.error("Feil ved henting av journalpost: Journalpostid ikke gyldig!")
            throw RuntimeException("Feil ved henting av journalpost: Journalpostid ikke gyldig!", e)
        }

    }
}
