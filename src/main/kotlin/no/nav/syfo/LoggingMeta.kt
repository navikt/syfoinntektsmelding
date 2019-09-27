package no.nav.syfo

data class LoggingMeta(
    val sykmeldingId: String,
    val journalpostId: String,
    val hendelsesId: String
)

class TrackableException(override val cause: Throwable, val loggingMeta: LoggingMeta) : RuntimeException()

suspend fun <O> wrapExceptions(loggingMeta: LoggingMeta, block: suspend () -> O): O {
    try {
        return block()
    } catch (e: Exception) {
        throw TrackableException(e, loggingMeta)
    }
}
