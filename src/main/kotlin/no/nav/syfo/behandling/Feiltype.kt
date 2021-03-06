package no.nav.syfo.behandling

enum class Feiltype(var navn: String) {
    USPESIFISERT("uspesifisert"),
    JMS("jms"),
    AZUREADTOKEN("azureadtoken"),
    TOKEN("token"),
    AKTØR_IKKE_FUNNET("aktor_ikkefunnet"),
    AKTØR_LISTE_TOM("aktor_listetom"),
    AKTØR_OPPSLAG_FEILET("aktor_oppslagfeil"),
    AKTØR_FEIL("aktor_feil"),
    DOKUMENT_SIKKERHETSBEGRENSNING("dokument_sikkerhet"),
    DOKUMENT_IKKE_FUNNET("dokument_ikkefunnet"),
    DOKUMENT_FEILET("dokument_feilet"),
    INNGÅENDE_MANGLER_KANALREFERANSE("mangler_kanalreferanse"),
    INNGÅENDE_SIKKERHETSBEGRENSNING("inn_sikkerhet"),
    INNGÅENDE_IKKE_INNGÅENDE("inn_ikkeinn"),
    INNGÅENDE_IKKE_FUNNET("inn_ikkefunnet"),
    INNGÅENDE_UGYLDIG_KALL("inn_ugyldig"),
    OPPDATERINNGÅENDE_UGYLDIG("oppdater_ugyldig"),
    OPPDATERINNGÅENDE_IKKE_FUNNET("oppdater_ikkefunnet"),
    OPPDATERINNGÅENDE_IKKE_MULIG("oppdater_ikkemulig"),
    OPPDATERINNGÅENDE_IKKE_INNGÅENDE("oppdater_ikkeinn"),
    OPPDATERINNGÅENDE_SIKKERHETSBEGRENSNING("oppdater_sikkerhet"),
    FERDIGSTILL_UGYLDIG("ferdigstill_ugyldig"),
    FERDIGSTILL_IKKE_FUNNET("ferdigstill_ikkefunnet"),
    FERDIGSTILL_IKKE_MULIG("ferdigstill_ikkemulig"),
    FERDIGSTILL_IKKE_INNGÅENDE("ferdigstill_ikkeinn"),
    FERDIGSTILL_SIKKERHETSBEGRENSNING("ferdigstill_sikkerhet"),
    OPPGAVE_HENT("oppgave_hent"),
    OPPGAVE_OPPRETT_VANLIG("oppgave_opprett"),
    OPPGAVE_OPPRETT_FORDELING("oppgave_fordeling"),
    BEHANDLENDE_UGYLDIG_INPUT("behandlede_ugyldig"),
    BEHANDLENDE_INGEN_AKTIV_ENHET("behandlede_ingenaktiv"),
    BEHANDLENDE_SIKKERHETSBEGRENSNING("behandlede_sikkerhet"),
    BEHANDLENDE_IKKE_FUNNET("behandlede_ikkefunnet"),
    BEHANDLENDE_FEILET("behandlede_feilet"),
    SAK_FEILET("sak_feilet"),
    SAK_RESPONSE("sak_response");
}
