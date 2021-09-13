package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import log
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.syfo.behandling.BehandlendeEnhetFeiletException
import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.client.norg.ArbeidsfordelingResponse
import no.nav.syfo.client.norg.ArbeidsfordelingRequest
import no.nav.syfo.client.norg.Norg2Client
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.Metrikk
import java.time.LocalDate

const val SYKEPENGER_UTLAND = "4474"
const val SYKEPENGER = "SYK";

class BehandlendeEnhetConsumer(
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client,
    private val metrikk: Metrikk
)  {

    var log = log()

    fun hentBehandlendeEnhet(fnr: String, uuid: String, tidspunkt: LocalDate = LocalDate.now()): String {
        val geografiskTilknytning = hentGeografiskTilknytning(fnr)

        val criteria = ArbeidsfordelingRequest(
            tema = SYKEPENGER,
            diskresjonskode = geografiskTilknytning?.diskresjonskode,
            geografiskOmraade = geografiskTilknytning?.geografiskTilknytning
        )

        val callId = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID)

        try {
            val arbeidsfordelinger = runBlocking {
                norg2Client.hentAlleArbeidsfordelinger(criteria, callId)
            }
            log.info("Fant enheter: " + arbeidsfordelinger.toString() )
            val behandlendeEnhet = finnAktivBehandlendeEnhet(
                arbeidsfordelinger,
                geografiskTilknytning?.geografiskTilknytning,
                tidspunkt
            )
            if (SYKEPENGER_UTLAND == behandlendeEnhet) {
                metrikk.tellInntektsmeldingSykepengerUtland()
            }
            log.info("Fant geografiskTilknytning ${geografiskTilknytning.geografiskTilknytning} med behandlendeEnhet $behandlendeEnhet for inntektsmelding $uuid");
            return behandlendeEnhet
        } catch (e: RuntimeException) {
            log.error("Klarte ikke å hente behandlende enhet!", e)
            throw BehandlendeEnhetFeiletException(e)
        }
    }

    fun hentGeografiskTilknytning(fnr: String): GeografiskTilknytningData {
        pdlClient.fullPerson(fnr).let {
            return GeografiskTilknytningData(
                geografiskTilknytning = it?.hentGeografiskTilknytning?.hentTilknytning(),
                diskresjonskode = it?.hentPerson?.trekkUtDiskresjonskode()
            )
        }
    }

}

fun finnAktivBehandlendeEnhet(arbeidsfordelinger: List<ArbeidsfordelingResponse>, geografiskTilknytning: String?, tidspunkt: LocalDate): String {
    return arbeidsfordelinger
        .stream()
        .filter { it.status === "Aktiv" }
        .filter { it.enhetNr !== null }
        .findFirst().orElseThrow { IngenAktivEnhetException(geografiskTilknytning, null) }.enhetNr!!
}
