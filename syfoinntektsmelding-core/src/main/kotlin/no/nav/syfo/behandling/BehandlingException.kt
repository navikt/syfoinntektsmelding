package no.nav.syfo.behandling

open class BehandlingException(open var feiltype: Feiltype, message: String) : RuntimeException(message)

// Azure
open class AzureAdTokenException(statusCode: Int) : BehandlingException(Feiltype.AZUREADTOKEN, "Henting av token fra Azure AD feiler med HTTP status $statusCode")

// TokenConsumer
open class TokenException(statusCode: Int) : BehandlingException(Feiltype.TOKEN, "Henting av token feiler med HTTP status $statusCode")

// AktorConsumer
open class AktørException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)
open class TomAktørListeException() : AktørException(Feiltype.AKTØR_LISTE_TOM, "Fikk tom liste for aktørId")
open class FantIkkeAktørException() : AktørException(Feiltype.AKTØR_IKKE_FUNNET, "Fant ikke aktørId")
open class AktørOppslagException() : AktørException(Feiltype.AKTØR_OPPSLAG_FEILET, "Feil ved oppslag i aktørtjenesten for aktørId")
open class AktørKallResponseException(statusCode: Int) : AktørException(Feiltype.AKTØR_FEIL, "Kall mot aktørregister for aktørId feiler med http status $statusCode")

// InngaaendeJournalConsumer
open class InngaaendeJournalConsumerException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)
open class HentJournalpostSikkerhetsbegrensningException(journalpostId: String) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning for journalpostId $journalpostId!")
open class HentJournalpostJournalpostIkkeInngaaendeException(journalpostId: String) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_IKKE_INNGÅENDE, "Ikke inngående journalpostId $journalpostId!")
open class HentJournalpostJournalpostIkkeFunneteException(journalpostId: String) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!")
open class HentJournalpostUgyldigInputException(journalpostId: String) : InngaaendeJournalConsumerException(Feiltype.INNGÅENDE_UGYLDIG_KALL, "Ugyldig journalpostId $journalpostId!")

// BehandleInngaaendeJournalConsumer
open class InngaaendeJournalException(override var feiltype: Feiltype, override var message: String) : BehandlingException(feiltype, message)
open class OppdaterJournalpostUgyldigInputException(journalpostId: String) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_UGYLDIG, "Ugyldig input for journalpostId $journalpostId!")
open class OppdaterJournalpostObjektIkkeFunnetException(journalpostId: String) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!")
open class OppdaterJournalpostOppdateringIkkeMuligException(journalpostId: String) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_MULIG, "Ikke mulig å oppdatere journalpostId $journalpostId!")
open class OppdaterJournalpostJournalpostIkkeInngaaendeException(journalpostId: String) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_IKKE_INNGÅENDE, "Kan ikke oppdatere fordi journalpost ikke er inngående $journalpostId!")
open class OppdaterJournalpostSikkerhetsbegrensningException(journalpostId: String) : InngaaendeJournalException(Feiltype.OPPDATERINNGÅENDE_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved journalføring for journalpostId $journalpostId!")

open class FerdigstillJournalfoeringUgyldigInputException(journalpostId: String) : InngaaendeJournalException(Feiltype.FERDIGSTILL_UGYLDIG, "Ugyldig input for journalpostId $journalpostId!")
open class FerdigstillJournalfoeringObjektIkkeFunnetException(journalpostId: String) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_FUNNET, "Fant ikke journalpostId $journalpostId!")
open class FerdigstillJournalfoeringFerdigstillingIkkeMuligException(journalpostId: String) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_MULIG, "Umulig å ferdigstille journalføring for journalpostId $journalpostId!")
open class FerdigstillJournalfoeringJournalpostIkkeInngaaendeException(journalpostId: String) : InngaaendeJournalException(Feiltype.FERDIGSTILL_IKKE_INNGÅENDE, "Kan ikke oppdatere fordi journalpost ikke er inngående $journalpostId!")
open class FerdigstillJournalfoeringSikkerhetsbegrensningException(journalpostId: String) : InngaaendeJournalException(Feiltype.FERDIGSTILL_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved journalføring for journalpostId $journalpostId!")

// OppgaveClient
open class OppgaveException(override var feiltype: Feiltype, message: String) : BehandlingException(feiltype, message)
open class HentOppgaveException(journalpostId: String,  oppgaveType: String) : OppgaveException(Feiltype.OPPGAVE_HENT, "Feil ved oppslag til oppgave for journalpostId $journalpostId med oppgavetype $oppgaveType!")
open class OpprettOppgaveException(journalpostId: String) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_VANLIG, "Klarte ikke opprette oppgave for journalpostId $journalpostId!")
open class OpprettFordelingsOppgaveException(journalpostId: String) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_FORDELING, "Klarte ikke opprette fordelingsoppgave for journalpostId $journalpostId!")

// BehandlendeEnhetConsumer
open class BehandlendeEnhetException(override var feiltype: Feiltype, message: String) : BehandlingException(feiltype, message)
open class FinnBehandlendeEnhetListeUgyldigInputException() : BehandlendeEnhetException(Feiltype.BEHANDLENDE_UGYLDIG_INPUT, "Feil ved henting av brukers forvaltningsenhet")
open class IngenAktivEnhetException(geografiskTilknytning: String?) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_INGEN_AKTIV_ENHET, "Fant ingen aktiv enhet for $geografiskTilknytning")
open class HentGeografiskTilknytningSikkerhetsbegrensingException() : BehandlendeEnhetException(Feiltype.BEHANDLENDE_SIKKERHETSBEGRENSNING, "Feil ved henting av geografisk tilknytning")
open class HentGeografiskTilknytningPersonIkkeFunnetException() : BehandlendeEnhetException(Feiltype.BEHANDLENDE_IKKE_FUNNET, "Feil ved henting av geografisk tilknytning")
open class BehandlendeEnhetFeiletException() : BehandlendeEnhetException(Feiltype.BEHANDLENDE_FEILET, "Feil ved henting av geografisk tilknytning")

// JournalConsumer
open class JournalException(override var feiltype: Feiltype, message: String) : BehandlingException(feiltype, message)
open class HentDokumentSikkerhetsbegrensningException(journalpostId: String) : JournalException(Feiltype.DOKUMENT_SIKKERHETSBEGRENSNING, "Sikkerhetsbegrensning ved henting av journalpostId $journalpostId!")
open class HentDokumentIkkeFunnetException(journalpostId: String) : JournalException(Feiltype.DOKUMENT_IKKE_FUNNET, "Fant ikke dokument for journalpostId $journalpostId!")
open class HentDokumentFeiletException(journalpostId: String) : JournalException(Feiltype.DOKUMENT_FEILET, "Kall til journal feilet for journalpostId $journalpostId!")

// SakConsumer
open class SakException(override var feiltype: Feiltype, message: String) : BehandlingException(feiltype, message)
open class SakResponseException(aktørId: String, statusCode: Int) : SakException(Feiltype.SAK_RESPONSE, "Kall mot syfonarmesteleder feiler med status $statusCode for aktørId $aktørId")
open class SakFeilException(aktørId: String) : SakException(Feiltype.SAK_FEILET, "Uventet feil ved henting av nærmeste leder for aktørId $aktørId")



