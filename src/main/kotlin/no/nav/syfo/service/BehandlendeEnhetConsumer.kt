package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.utils.logger
import no.nav.syfo.behandling.BehandlendeEnhetFeiletException
import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.client.norg.ArbeidsfordelingRequest
import no.nav.syfo.client.norg.ArbeidsfordelingResponse
import no.nav.syfo.client.norg.Norg2Client
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.Metrikk

const val SYKEPENGER_UTLAND = "4474"
const val SYKEPENGER = "SYK"

class BehandlendeEnhetConsumer(
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client,
    private val metrikk: Metrikk
) {
    private val logger = this.logger()

    fun hentBehandlendeEnhet(fnr: String, uuid: String): String {
        val geografiskTilknytning = hentGeografiskTilknytning(fnr)

        val criteria = ArbeidsfordelingRequest(
            tema = SYKEPENGER,
            diskresjonskode = geografiskTilknytning.diskresjonskode,
            geografiskOmraade = geografiskTilknytning.geografiskTilknytning
        )

        val callId = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID)

        try {
            val arbeidsfordelinger = runBlocking {
                norg2Client.hentAlleArbeidsfordelinger(criteria, callId)
            }
            logger.info("Fant enheter: " + arbeidsfordelinger.toString())
            val behandlendeEnhet = finnAktivBehandlendeEnhet(
                arbeidsfordelinger,
                geografiskTilknytning.geografiskTilknytning
            )
            if (SYKEPENGER_UTLAND == behandlendeEnhet) {
                metrikk.tellInntektsmeldingSykepengerUtland()
            }
            logger.info("Fant geografiskTilknytning ${geografiskTilknytning.geografiskTilknytning} med behandlendeEnhet $behandlendeEnhet for inntektsmelding $uuid")
            return behandlendeEnhet
        } catch (e: RuntimeException) {
            logger.error("Klarte ikke å hente behandlende enhet!", e)
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

fun finnAktivBehandlendeEnhet(arbeidsfordelinger: List<ArbeidsfordelingResponse>, geografiskTilknytning: String?): String {
    return arbeidsfordelinger
        .stream()
        .findFirst().orElseThrow { IngenAktivEnhetException(geografiskTilknytning, null) }.enhetNr!!
}
