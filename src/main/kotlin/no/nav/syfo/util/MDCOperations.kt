package no.nav.syfo.util

import java.security.SecureRandom
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class MDCOperations {
    companion object {
        var log = logger<MDCOperations>()
        const val MDC_CALL_ID = "callId"
        const val MDC_USER_ID = "userId"
        const val MDC_CONSUMER_ID = "consumerId"
        private val RANDOM = SecureRandom()

        fun generateCallId(): String = "CallId_${getRandomNumber()}_${getSystemTime()}"

        fun getFromMDC(key: String): String? {
            val value = MDC.get(key)
            log.debug("Getting key: $key from MDC with value: $value")
            return value
        }

        fun putToMDC(key: String, value: String) {
            log.debug("Putting value: $value on MDC with key: $key")
            MDC.put(key, value)
        }

        fun remove(key: String) {
            log.debug("Removing key: $key")
            MDC.remove(key)
        }

        private fun getRandomNumber(): Int = RANDOM.nextInt(Int.MAX_VALUE)
        private fun getSystemTime(): Long = System.currentTimeMillis()
    }
}

inline fun <reified T> logger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}
