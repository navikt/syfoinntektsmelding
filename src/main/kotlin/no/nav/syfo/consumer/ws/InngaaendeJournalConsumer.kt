package no.nav.syfo.consumer.ws

import log
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.graphql.DokarkivGraphQLClient
import no.nav.syfo.behandling.HentJournalpostJournalpostIkkeFunneteException
import no.nav.syfo.behandling.HentJournalpostJournalpostIkkeInngaaendeException
import no.nav.syfo.behandling.HentJournalpostSikkerhetsbegrensningException
import no.nav.syfo.behandling.HentJournalpostUgyldigInputException
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.*

class InngaaendeJournalConsumer(private val inngaaendeJournal: DokarkivGraphQLClient) {

    var log = log()

    fun hentDokumentId(journalpostId: String): InngaaendeJournal? {
        try {

            val inngaaendeJournalpost = inngaaendeJournal.getJournalpost(journalpostId)
            return if (inngaaendeJournalpost != null) {
                InngaaendeJournal(
                    dokumentId = inngaaendeJournalpost.dokumenter.first().dokumentInfoId,
                    status = inngaaendeJournalpost.journalstatus,
                    mottattDato = inngaaendeJournalpost.datoOpprettet
                )
            } else null
        } catch(e: Exception){
            log.error("Feil ved henting av journalpost")
            throw e
        }
    }
}
