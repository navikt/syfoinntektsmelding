package no.nav.syfo.consumer.mq

import log
import org.springframework.stereotype.Service
import org.springframework.util.ErrorHandler

@Service
class MQErrorHandler : ErrorHandler {

    val log = log()

    override fun handleError(throwable: Throwable) {
        log.error("Feil ved mottak av inngående journalpost", throwable)
    }
}
