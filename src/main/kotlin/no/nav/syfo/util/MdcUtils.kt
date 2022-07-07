package no.nav.syfo.util

import org.slf4j.MDC
import java.security.SecureRandom
import java.util.UUID

private val RANDOM = SecureRandom()

object MdcUtils {
    private object Keys {
        const val CALL_ID = "callId"
    }

    fun withCallId(fn: () -> Unit): Unit =
        withLogField(
            Keys.CALL_ID to newCallId(),
            fn,
        )

    fun withCallIdAsUuid(fn: () -> Unit): Unit =
        withLogField(
            Keys.CALL_ID to uuid4(),
            fn,
        )

    fun getCallId(): String? =
        MDC.get(Keys.CALL_ID)

    private fun withLogField(logField: Pair<String, String>, fn: () -> Unit) {
        val (key, value) = logField
        MDC.put(key, value)
        try {
            fn()
        } finally {
            MDC.remove(key)
        }
    }
}

fun newCallId(): String {
    val randomNumber = RANDOM.nextInt(Int.MAX_VALUE)
    val systemTime = System.currentTimeMillis()
    return "CallId_${randomNumber}_$systemTime"
}

private fun uuid4(): String =
    UUID.randomUUID().toString()
