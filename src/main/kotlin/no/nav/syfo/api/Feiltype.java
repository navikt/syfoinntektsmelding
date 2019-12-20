package no.nav.syfo.api;

public enum Feiltype {

    HENT_INNTEKTSMELDING_FRA_JOURNALPOST("hentjournal"),
    FERDIGSTILL_JOURNALPOST("ferdigstill"),
    HENT_AKTØR_ID("hentaktor"),
    SAKSBEHANDLING("saksbehandling"),
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
