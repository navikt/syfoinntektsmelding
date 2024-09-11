package no.nav.syfo.util

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun PdlClient.getAktørid(fnr: String): String? {
    // TODO: ktor 2 har retry-mulighet i httpClient, oppgrader og erstatt denne quick-fixen...
    val retryMs = 500L
    return try {
        runBlocking { hentAktoerID(fnr) }
    } catch (e: Exception) {
        sikkerLogger().warn("Kall til PDL feilet, venter $retryMs ms og forsøker igjen..")
        Thread.sleep(retryMs)
        try {
            runBlocking { hentAktoerID(fnr) }
        } catch (e2: Exception) {
            null
        }
    }
}
