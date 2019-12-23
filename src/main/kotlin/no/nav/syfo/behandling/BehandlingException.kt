package no.nav.syfo.behandling

open class BehandlingException(open var feiltype: Feiltype, message: String) : RuntimeException(message)

// Azure
open class AzureAdTokenException(statusCode: Int) : BehandlingException(Feiltype.AZUREADTOKEN, "Henting av token fra Azure AD feiler med HTTP status $statusCode")

// TokenConsumer
open class TokenException(statusCode: Int) : BehandlingException(Feiltype.TOKEN, "Henting av token feiler med HTTP-$statusCode")

// AktorConsumer
open class AktørException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)
open class TomAktørListeException(aktørId: String) : AktørException(Feiltype.AKTØR_LISTE_TOM, "Fikk tom liste for aktørId $aktørId")
open class FantIkkeAktørException(aktørId: String) : AktørException(Feiltype.AKTØR_IKKE_FUNNET, "Fant ikke aktørId $aktørId")
open class AktørOppslagException(aktørId: String, exception: Exception) : AktørException(Feiltype.AKTØR_OPPSLAG_FEILET, "Feil ved oppslag i aktørtjenesten for aktørId $aktørId")
open class AktørKallResponseException(aktørId: String, statusCode: Int) : AktørException(Feiltype.AKTØR_FEIL, "Kall mot aktørregister for aktørId $aktørId feiler med http status $statusCode")

// InngaaendeJournalConsumer
open class InngaaendeJournalConsumerException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)
open class HentJournalpostSikkerhetsbegrensningException(journalpostId: String, exception: Exception) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning for journalpostId $journalpostId!")
open class HentJournalpostJournalpostIkkeInngaaendeException(journalpostId: String, exception: Exception) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_IKKE_INNGÅENDE, "Ikke inngående journalpostId $journalpostId!")
open class HentJournalpostJournalpostIkkeFunneteException(journalpostId: String, exception: Exception) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!")
open class HentJournalpostUgyldigInputException(journalpostId: String, exception: Exception) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_UGYLDIG_KALL, "Ugyldig journalpostId $journalpostId!")

// BehandleInngaaendeJournalConsumer
open class InngaaendeJournalException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)
open class OppdaterJournalpostUgyldigInputException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_UGYLDIG, "Ugyldig input for journalpostId $journalpostId!")
open class OppdaterJournalpostObjektIkkeFunnetException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!")
open class OppdaterJournalpostOppdateringIkkeMuligException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_MULIG, "Ikke mulig å oppdatere journalpostId $journalpostId!")
open class OppdaterJournalpostJournalpostIkkeInngaaendeException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_INNGÅENDE, "Kan ikke oppdatere fordi journalpost ikke er inngående $journalpostId!")
open class OppdaterJournalpostSikkerhetsbegrensningException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved journalføring for journalpostId $journalpostId!")

open class FerdigstillJournalfoeringUgyldigInputException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.FERDIGSTILL_UGYLDIG, "Ugyldig input for journalpostId $journalpostId!")
open class FerdigstillJournalfoeringObjektIkkeFunnetException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!")
open class FerdigstillJournalfoeringFerdigstillingIkkeMuligException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_MULIG, "Umulig å ferdigstille journalføring for journalpostId $journalpostId!")
open class FerdigstillJournalfoeringJournalpostIkkeInngaaendeException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_INNGÅENDE, "Kan ikke oppdatere fordi journalpost ikke er inngående $journalpostId!")
open class FerdigstillJournalfoeringSikkerhetsbegrensningException(journalpostId: String, exception: Exception) : InngaaendeJournalException(Feiltype.FERDIGSTILL_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved journalføring for journalpostId $journalpostId!")

// OppgaveClient
open class OppgaveException(override var feiltype: Feiltype, message: String) : BehandlingException(feiltype, message)
open class HentOppgaveException(journalpostId: String,  oppgaveType: String, exception: Exception) : OppgaveException(Feiltype.OPPGAVE_HENT, "Feil ved oppslag til oppgave for journalpostId $journalpostId med oppgavetype $oppgaveType!")
open class OpprettOppgaveException(journalpostId: String, exception: Exception) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_VANLIG, "Klarte ikke opprette oppgave for journalpostId $journalpostId!")
open class OpprettFordelingsOppgaveException(journalpostId: String, exception: Exception) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_FORDELING, "Klarte ikke opprette fordelingsoppgave for journalpostId $journalpostId!")

// BehandlendeEnhetConsumer
open class BehandlendeEnhetException(override var feiltype: Feiltype, message: String) : BehandlingException(feiltype, message)
open class FinnBehandlendeEnhetListeUgyldigInputException(journalpostId: String, exception: Exception) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_UGYLDIG_INPUT, "Feil ved henting av brukers forvaltningsenhet")
open class IngenAktivEnhetException(geografiskTilknytning: String?) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_INGEN_AKTIV_ENHET, "Fant ingen aktiv enhet for $geografiskTilknytning")
open class HentGeografiskTilknytningSikkerhetsbegrensingException(geografiskTilknytning: String, exception: Exception) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_SIKKERHETSBEGRENSNING, "Feil ved henting av geografisk tilknytning")
open class HentGeografiskTilknytningPersonIkkeFunnetException(exception: Exception) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_IKKE_FUNNET, "Feil ved henting av geografisk tilknytning")
open class BehandlendeEnhetFeiletException(exception: Exception) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_FEILET, "Feil ved henting av geografisk tilknytning")

// JournalConsumer
open class JournalException(override var feiltype: Feiltype, message: String) : BehandlingException(feiltype, message)
open class HentDokumentSikkerhetsbegrensningException(journalpostId: String, exception: Exception) : JournalException(Feiltype.AKTØR_IKKE_FUNNET, "Sikkerhetsbegrensning ved henting av journalpostId $journalpostId!")
open class HentDokumentIkkeFunnetException(journalpostId: String, exception: Exception) : JournalException(Feiltype.AKTØR_IKKE_FUNNET, "Fant ikke dokument for journalpostId $journalpostId!")
open class HentDokumentFeiletException(journalpostId: String, exception: Exception) : JournalException(Feiltype.AKTØR_IKKE_FUNNET, "Kall til journal feilet for journalpostId $journalpostId!")

// SakConsumer
open class SakException(override var feiltype: Feiltype, message: String) : BehandlingException(Feiltype.AKTØR_IKKE_FUNNET, "Uventet feil ved henting av nærmeste leder")
open class SakResponseException(aktørId: String, statusCode: Int) : SakException(Feiltype.SAK_RESPONSE, "Kall mot syfonarmesteleder feiler med status $statusCode for aktørId $aktørId")
open class SakFeilException(aktørId: String, exception: Exception) : SakException(Feiltype.SAK_FEILET, "Uventet feil ved henting av nærmeste leder")



