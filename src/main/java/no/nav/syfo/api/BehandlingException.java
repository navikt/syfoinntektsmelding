package no.nav.syfo.api;


public class BehandlingException extends RuntimeException {
    String arkivReferanse;
    public BehandlingException(String arkivReferanse, Exception ex) {
        super(ex);
    }

    public String getArkivReferanse() {
        return arkivReferanse;
    }
}
