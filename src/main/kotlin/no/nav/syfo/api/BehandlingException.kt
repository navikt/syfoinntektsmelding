package no.nav.syfo.api

import org.springframework.http.HttpStatus

open class BehandlingException(open var feiltype: Feiltype, message: String) : RuntimeException(message)

// AktorConsumer
open class AktorConsumerException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)

open class TomAktørListeException(aktørId: String) : AktorConsumerException(Feiltype.HENT_AKTØR_ID, "Fikk tom liste for $aktørId")
open class FantIkkeAktørException(aktørId: String) : AktorConsumerException(Feiltype.HENT_AKTØR_ID, "Fant ikke aktør $aktørId")
open class AktørOppslagException(aktørId: String, ex: Exception) : AktorConsumerException(Feiltype.HENT_AKTØR_ID, "Fant ikke aktør $aktørId")
open class AktørKallResponseException(aktørId: String, statusCode: Integer) : AktorConsumerException(Feiltype.HENT_AKTØR_ID, "Kall mot aktørregister feiler med http status $statusCode")

// InngaaendeJournalConsumer
open class InngaaendeJournalConsumerException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)

open class HentJournalpostSikkerhetsbegrensningException(exception: Exception) : InngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av journalpost: Sikkerhetsbegrensning!")
open class HentJournalpostJournalpostIkkeInngaaendeException(exception: Exception) : InngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av journalpost: Journalpost er ikke inngaaende!")
open class HentJournalpostJournalpostIkkeFunneteException(exception: Exception) : InngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av journalpost: Journalpost er ikke inngaaende!")
open class HentJournalpostUgyldigInputException(exception: Exception) : InngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av journalpost: Journalpostid ikke gyldig!")


// BehandleInngaaendeJournalConsumer
open class BehandleInngaaendeJournalConsumerException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)
open class OppdaterJournalpostUgyldigInputException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved oppdatering av journalpost: $journalpostId - Ugyldig input!")
open class OppdaterJournalpostObjektIkkeFunnetException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved oppdatering av journalpost: $journalpostId - Journalpost ikke funnet!")
open class OppdaterJournalpostOppdateringIkkeMuligException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved oppdatering av journalpost: $journalpostId - Journalpost ikke funnet!")
open class OppdaterJournalpostJournalpostIkkeInngaaendeException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved oppdatering av journalpost: $journalpostId - Journalpost er ikke inngående!")
open class OppdaterJournalpostSikkerhetsbegrensningException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved oppdatering av journalpost: $journalpostId - Sikkerhetsbegrensning!")

open class FerdigstillJournalfoeringUgyldigInputException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Ugyldig innput!")
open class FerdigstillJournalfoeringObjektIkkeFunnetException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Journalpost ikke funnet!")
open class FerdigstillJournalfoeringJournalpostIkkeInngaaendeException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Ikke inngående!")
open class FerdigstillJournalfoeringSikkerhetsbegrensningException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Sikkerhetsbegrensing!")
open class FerdigstillJournalfoeringFerdigstillingIkkeMuligException(journalpostId: String, exception: Exception) : BehandleInngaaendeJournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Ikke mulig å ferdigstille!")

// Azure
open class AzureAdTokenConsumerException(val statusCode: HttpStatus) : BehandlingException(Feiltype.HENT_AKTØR_ID, "Henting av token fra Azure AD feiler med HTTP-$statusCode")

// OppgaveClient
open class OppgaveClientException(override var feiltype: Feiltype, journalpostId: String, exception: Exception) : BehandlingException(feiltype, "")

open class HentOppgaveException(journalpostId: String,  oppgaveType: String, exception: Exception) : OppgaveClientException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Ikke mulig å ferdigstille!")
open class OpprettOppgaveException(journalpostId: String, exception: Exception) : OppgaveClientException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Ikke mulig å ferdigstille!")
open class OpprettFordelingsOppgaveException(journalpostId: String, exception: Exception) : OppgaveClientException(Feiltype.HENT_AKTØR_ID, "Feil ved ferdigstilling av journalpost: $journalpostId - Ikke mulig å ferdigstille!")

// BehandlendeEnhetConsumer
open class BehandlendeEnhetConsumerException(override var feiltype: Feiltype, journalpostId: String, exception: Exception) : BehandlingException(feiltype, exception)

open class FinnBehandlendeEnhetListeUgyldigInputException(journalpostId: String, exception: Exception) : BehandlendeEnhetConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av brukers forvaltningsenhet")
open class IngenAktivEnhetException(geografiskTilknytning: String?) : BehandlendeEnhetConsumerException(Feiltype.HENT_AKTØR_ID, "Fant ingen aktiv enhet for $geografiskTilknytning")
open class HentGeografiskTilknytningSikkerhetsbegrensingException(geografiskTilknytning: String, exception: Exception) : BehandlendeEnhetConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av geografisk tilknytning")
open class HentGeografiskTilknytningPersonIkkeFunnetException(exception: Exception) : BehandlendeEnhetConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av geografisk tilknytning")
open class BehandlendeEnhetFeiletException(exception: Exception) : BehandlendeEnhetConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av geografisk tilknytning")

// JournalConsumer
open class JournalConsumerException(override var feiltype: Feiltype, journalpostId: String, exception: Exception) : BehandlingException(feiltype, exception)

open class HentDokumentSikkerhetsbegrensningException(journalpostId: String, exception: Exception) : JournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av dokument: Sikkerhetsbegrensning!")
open class HentDokumentDokumentIkkeFunnetException(journalpostId: String, exception: Exception) : JournalConsumerException(Feiltype.HENT_AKTØR_ID, "Feil ved henting av journalpost: Dokument ikke funnet!")




