package no.nav.syfo.domain

import no.nav.syfo.client.saf.model.SafAvsenderMottaker

data class InngaendeJournalpost(
    val fnr: String,
    val journalpostId: String,
    val dokumentId: String,
    val behandlendeEnhetId: String,
    val arbeidsgiverOrgnummer: String? = null,
    val arbeidsgiverNavn: String = "Arbeidsgiver",
    val arbeidsgiverPrivat: String? = null,
    val safAvsenderMottaker: SafAvsenderMottaker? = null,
)
