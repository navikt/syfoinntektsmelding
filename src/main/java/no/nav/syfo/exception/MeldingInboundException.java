package no.nav.syfo.exception;

public class MeldingInboundException extends RuntimeException {
    public MeldingInboundException(String message) {
        super(message);
    }

    public MeldingInboundException(String message, Throwable cause) {
        super(message, cause);
    }
}
