package no.nav.syfo.behandling

open class BehandlingException(val feiltype: Feiltype, message: String, cause: Exception?) : RuntimeException(message, cause)

// Azure
open class AzureAdTokenException(statusCode: Int, cause: java.lang.Exception?) : BehandlingException(Feiltype.AZUREADTOKEN, "Henting av token fra Azure AD feiler med HTTP status $statusCode", null)

// TokenConsumer
open class TokenException(statusCode: Int, causedBy: Exception?) : BehandlingException(Feiltype.TOKEN, "Henting av token feiler med HTTP status $statusCode", causedBy)

// AktorClient
open class AktørException(feiltype: Feiltype,  message: String, causedBy: Exception?) : BehandlingException(feiltype, message, causedBy)
open class TomAktørListeException(causedBy: java.lang.Exception?) : AktørException(Feiltype.AKTØR_LISTE_TOM, "Fikk tom liste for aktørId", causedBy )
open class FantIkkeAktørException(causedBy: java.lang.Exception?) : AktørException(Feiltype.AKTØR_IKKE_FUNNET, "Fant ikke aktørId", causedBy )
open class AktørOppslagException(causedBy: java.lang.Exception?) : AktørException(Feiltype.AKTØR_OPPSLAG_FEILET, "Feil ved oppslag i aktørtjenesten for aktørId", causedBy )
open class AktørKallResponseException(statusCode: Int, causedBy: java.lang.Exception?) : AktørException(Feiltype.AKTØR_FEIL, "Kall mot aktørregister for aktørId feiler med http status $statusCode", causedBy )

// InngaaendeJournalConsumer
open class InngaaendeJournalConsumerException(feiltype: Feiltype, message: String, private val causedBy: Exception?) : BehandlingException(feiltype, message, causedBy)
open class HentJournalpostSikkerhetsbegrensningException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning for journalpostId $journalpostId!", cause )
open class HentJournalpostJournalpostIkkeInngaaendeException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_IKKE_INNGÅENDE, "Ikke inngående journalpostId $journalpostId!", cause)
open class HentJournalpostJournalpostIkkeFunneteException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!", cause )
open class HentJournalpostUgyldigInputException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_UGYLDIG_KALL, "Ugyldig journalpostId $journalpostId!", cause )

// BehandleInngaaendeJournalConsumer
open class InngaaendeJournalException(feiltype: Feiltype, message: String, causedBy: Exception?) : BehandlingException(feiltype, message, causedBy)
open class OppdaterJournalpostUgyldigInputException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_UGYLDIG, "Ugyldig input for journalpostId $journalpostId!", cause )
open class OppdaterJournalpostObjektIkkeFunnetException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!", cause )
open class OppdaterJournalpostOppdateringIkkeMuligException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_MULIG, "Ikke mulig å oppdatere journalpostId $journalpostId!", cause )
open class OppdaterJournalpostJournalpostIkkeInngaaendeException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_INNGÅENDE, "Kan ikke oppdatere fordi journalpost ikke er inngående $journalpostId!", cause )
open class OppdaterJournalpostSikkerhetsbegrensningException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved journalføring for journalpostId $journalpostId!", cause )

open class FerdigstillJournalfoeringUgyldigInputException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.FERDIGSTILL_UGYLDIG, "Ugyldig input for journalpostId $journalpostId!", cause )
open class FerdigstillJournalfoeringObjektIkkeFunnetException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!", cause )
open class FerdigstillJournalfoeringFerdigstillingIkkeMuligException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_MULIG, "Umulig å ferdigstille journalføring for journalpostId $journalpostId!", cause )
open class FerdigstillJournalfoeringJournalpostIkkeInngaaendeException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_INNGÅENDE, "Kan ikke oppdatere fordi journalpost ikke er inngående $journalpostId!", cause )
open class FerdigstillJournalfoeringSikkerhetsbegrensningException(journalpostId: String, cause: java.lang.Exception?) : InngaaendeJournalException(Feiltype.FERDIGSTILL_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved journalføring for journalpostId $journalpostId!", cause)

// OppgaveClient
open class OppgaveException( feiltype: Feiltype, message: String, causedBy: Exception?) : BehandlingException(feiltype, message, causedBy)
open class HentOppgaveException(journalpostId: String, oppgaveType: String, cause: Exception?) : OppgaveException(Feiltype.OPPGAVE_HENT, "Feil ved oppslag til oppgave for journalpostId $journalpostId med oppgavetype $oppgaveType!", cause)
open class OpprettOppgaveException(journalpostId: String, cause: Exception?) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_VANLIG, "Klarte ikke opprette oppgave for journalpostId $journalpostId!", cause)
open class OpprettFordelingsOppgaveException(journalpostId: String, cause: Exception?) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_FORDELING, "Klarte ikke opprette fordelingsoppgave for journalpostId $journalpostId!", cause)

// BehandlendeEnhetConsumer
open class BehandlendeEnhetException( feiltype: Feiltype, message: String, cause: Exception?) : BehandlingException(feiltype, message, cause)
open class FinnBehandlendeEnhetListeUgyldigInputException(cause: Exception?) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_UGYLDIG_INPUT, "Feil ved henting av brukers forvaltningsenhet", cause)
open class IngenAktivEnhetException(geografiskTilknytning: String?, cause: java.lang.Exception?) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_INGEN_AKTIV_ENHET, "Fant ingen aktiv enhet for $geografiskTilknytning", cause )
open class HentGeografiskTilknytningSikkerhetsbegrensingException(cause: java.lang.Exception?) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_SIKKERHETSBEGRENSNING, "Feil ved henting av geografisk tilknytning", cause )
open class HentGeografiskTilknytningPersonIkkeFunnetException(cause: java.lang.Exception?) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_IKKE_FUNNET, "Feil ved henting av geografisk tilknytning", cause )
open class BehandlendeEnhetFeiletException(cause: java.lang.Exception?) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_FEILET, "Feil ved henting av geografisk tilknytning", cause )

// JournalConsumer
open class JournalException( feiltype: Feiltype, message: String, cause: Exception?) : BehandlingException(feiltype, message, cause)
open class HentDokumentSikkerhetsbegrensningException(journalpostId: String, cause: java.lang.Exception?) : JournalException(Feiltype.DOKUMENT_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved henting av journalpostId $journalpostId!", cause )
open class HentDokumentIkkeFunnetException(journalpostId: String, cause: java.lang.Exception?) : JournalException(Feiltype.DOKUMENT_IKKE_FUNNET, "Fant ikke dokument for journalpostId $journalpostId!", cause )
open class HentDokumentFeiletException(journalpostId: String, cause: java.lang.Exception?) : JournalException(Feiltype.DOKUMENT_FEILET, "Kall til journal feilet for journalpostId $journalpostId!", cause )

// SakConsumer
open class SakException( feiltype: Feiltype, message: String, cause: Exception?) : BehandlingException(feiltype, message, cause)
open class SakResponseException(aktørId: String, statusCode: Int, cause: Exception?) : SakException(Feiltype.SAK_RESPONSE, "Kall mot syfonarmesteleder feiler med status $statusCode for aktørId $aktørId", cause )
open class SakFeilException(aktørId: String, cause: java.lang.Exception?) : SakException(Feiltype.SAK_FEILET, "Uventet feil ved henting av nærmeste leder for aktørId $aktørId", cause )



