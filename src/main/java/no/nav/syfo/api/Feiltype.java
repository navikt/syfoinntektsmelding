package no.nav.syfo.api;

public enum Feiltype {

    HENT_INNTEKTSMELDING_FRA_JOURNALPOST("hentjournal"),
    HENT_AKTØR_ID("hentaktor"),
    FERDIGSTILL_JOURNALPOST("ferdigstill"),
    OPPGAVE("oppgave"),
    KAFKA("kafka"),
    LAGRE_BEHANDLING("lagrebehandling");

    String navn;
    Feiltype(String navn){
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }
}
