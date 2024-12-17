package no.nav.syfo.behandling

open class BehandlingException(
    val feiltype: Feiltype,
    message: String,
    cause: Exception?,
) : RuntimeException(message, cause)

// AktorClient
open class AktørException(
    feiltype: Feiltype,
    message: String,
    causedBy: Exception?,
) : BehandlingException(feiltype, message, causedBy)

open class FantIkkeAktørException(
    causedBy: java.lang.Exception?,
) : AktørException(
        Feiltype.AKTØR_IKKE_FUNNET,
        "Fant ikke aktørId",
        causedBy,
    )

// OppgaveClient
open class OppgaveException(
    feiltype: Feiltype,
    message: String,
    causedBy: Exception?,
) : BehandlingException(feiltype, message, causedBy)

open class HentOppgaveException(
    journalpostId: String,
    oppgaveType: String,
    cause: Exception?,
) : OppgaveException(
        Feiltype.OPPGAVE_HENT,
        "Feil ved oppslag til oppgave for journalpostId $journalpostId med oppgavetype $oppgaveType!",
        cause,
    )

open class OpprettOppgaveException(
    journalpostId: String,
    cause: Exception?,
) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_VANLIG, "Klarte ikke opprette oppgave for journalpostId $journalpostId!", cause)

open class OpprettFordelingsOppgaveException(
    journalpostId: String,
    cause: Exception?,
) : OppgaveException(Feiltype.OPPGAVE_OPPRETT_FORDELING, "Klarte ikke opprette fordelingsoppgave for journalpostId $journalpostId!", cause)

// BehandlendeEnhetConsumer
open class BehandlendeEnhetException(
    feiltype: Feiltype,
    message: String,
    cause: Exception?,
) : BehandlingException(feiltype, message, cause)

open class IngenAktivEnhetException(
    geografiskTilknytning: String?,
    cause: java.lang.Exception?,
) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_INGEN_AKTIV_ENHET, "Fant ingen aktiv enhet for $geografiskTilknytning", cause)

open class BehandlendeEnhetFeiletException(
    cause: java.lang.Exception?,
) : BehandlendeEnhetException(Feiltype.BEHANDLENDE_FEILET, "Feil ved henting av geografisk tilknytning", cause)

// JournalConsumer
open class JournalException(
    feiltype: Feiltype,
    message: String,
    cause: Exception?,
) : BehandlingException(feiltype, message, cause)

open class HentDokumentFeiletException(
    journalpostId: String,
    cause: java.lang.Exception?,
) : JournalException(Feiltype.DOKUMENT_FEILET, "Kall til journal feilet for journalpostId $journalpostId!", cause)
