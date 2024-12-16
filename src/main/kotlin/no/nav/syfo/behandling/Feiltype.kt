package no.nav.syfo.behandling

enum class Feiltype(
    var navn: String,
) {
    USPESIFISERT("uspesifisert"),
    AKTØR_IKKE_FUNNET("aktor_ikkefunnet"),
    DOKUMENT_FEILET("dokument_feilet"),
    OPPGAVE_HENT("oppgave_hent"),
    OPPGAVE_OPPRETT_VANLIG("oppgave_opprett"),
    OPPGAVE_OPPRETT_FORDELING("oppgave_fordeling"),
    BEHANDLENDE_INGEN_AKTIV_ENHET("behandlede_ingenaktiv"),
    BEHANDLENDE_FEILET("behandlede_feilet"),
}
