package no.nav.syfo.consumer.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;

@Service
@Slf4j
public class MQErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable throwable) {
        log.error("Feil ved mottak av inngående journalpost: ");
    }
}
