package no.nav.syfo.api;


public class BehandlingException extends RuntimeException {
    public BehandlingException(Exception ex) {
        super(ex);
    }
}
