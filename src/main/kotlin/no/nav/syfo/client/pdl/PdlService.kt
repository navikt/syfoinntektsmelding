package no.nav.syfo.client.pdl

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.pdl.PdlClient

class PdlService(
    private val pdlClient: PdlClient,
) {
    fun hentNavn(ident: String): String =
        runBlocking { pdlClient.personNavn(ident) }
            ?.fulltNavn()
            .orEmpty()

    fun hentAktoerId(ident: String): String? = runBlocking { pdlClient.hentAktoerID(ident) }

    fun hentGeografiskTilknytning(ident: String): GeografiskTilknytning =
        runBlocking { pdlClient.fullPerson(ident) }
            .let {
                GeografiskTilknytning(
                    diskresjonskode = it?.diskresjonskode,
                    geografiskTilknytning = it?.geografiskTilknytning,
                )
            }
}

data class GeografiskTilknytning(
    var diskresjonskode: String?,
    var geografiskTilknytning: String?,
)
