package no.nav.syfo.util

import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun PdlClient.getAktørid(fnr: String): String? {
    // TODO: ktor 2 har retry-mulighet i httpClient, oppgrader og erstatt denne quick-fixen...
    val retryMs = 500L
    return try {
        this.fullPerson(fnr)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
    } catch (e: Exception) {
        sikkerLogger().warn("Kall til PDL feilet, venter $retryMs ms og forsøker igjen..")
        Thread.sleep(retryMs)
        try {
            this.fullPerson(fnr)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        } catch (e2: Exception) {
            null
        }
    }
}
