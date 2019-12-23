package no.nav.syfo.behandling;

public enum Feiltype {

    AZUREADTOKEN("azureadtoken"),
    TOKEN("token"),

    AKTØR_IKKE_FUNNET("aktorikkefunnet"),
    AKTØR_LISTE_TOM("aktorlistetom"),
    AKTØR_OPPSLAG_FEILET("aktoroppslagfeil"),
    AKTØR_FEIL("aktorfeil"),

    INNGÅENDE_SIKKERHETSBEGRENSNING("innsikkerhet"),
    INNGÅENDE_IKKE_INNGÅENDE("innikkeinn"),
    INNGÅENDE_IKKE_FUNNET("innikkefunnet"),
    INNGÅENDE_UGYLDIG_KALL("innugyldig"),


    OPPDATERINNGÅENDE_UGYLDIG("oppdaterugyldig"),
    OPPDATERINNGÅENDE_IKKE_FUNNET("oppdaterikkefunnet"),
    OPPDATERINNGÅENDE_IKKE_MULIG("oppdaterikkemulig"),
    OPPDATERINNGÅENDE_IKKE_INNGÅENDE("oppdaterikkeinn"),
    OPPDATERINNGÅENDE_SIKKERHETSBEGRENSNING("oppdatersikkerhet"),

    FERDIGSTILL_UGYLDIG("ferdigstillugyldig"),
    FERDIGSTILL_IKKE_FUNNET("ferdigstillikkefunnet"),
    FERDIGSTILL_IKKE_MULIG("ferdigstillikkemulig"),
    FERDIGSTILL_IKKE_INNGÅENDE("ferdigstillikkeinn"),
    FERDIGSTILL_SIKKERHETSBEGRENSNING("ferdigstillsikkerhet"),

    OPPGAVE_HENT("oppgavehent"),
    OPPGAVE_OPPRETT_VANLIG("oppgaveopprett"),
    OPPGAVE_OPPRETT_FORDELING("oppgavefordeling"),

    BEHANDLENDE_UGYLDIG_INPUT("behandledeugyldig"),
    BEHANDLENDE_INGEN_AKTIV_ENHET("behandledeingenaktiv"),
    BEHANDLENDE_SIKKERHETSBEGRENSNING("behandledesikkerhet"),
    BEHANDLENDE_IKKE_FUNNET("behandledeikkefunnet"),
    BEHANDLENDE_FEILET("behandledefeilet"),

    SAK_FEILET("sakfeilet"),
    SAK_RESPONSE("sakresponse"),
    ;

    String navn;
    Feiltype(String navn){
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }
}
