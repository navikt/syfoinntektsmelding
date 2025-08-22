package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.behandling.BehandlendeEnhetFeiletException
import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.client.norg.ArbeidsfordelingRequest
import no.nav.syfo.client.norg.ArbeidsfordelingResponse
import no.nav.syfo.client.norg.Norg2Client
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.util.Metrikk

const val SYKEPENGER_UTLAND = "4474"
const val SYKEPENGER = "SYK"

class BehandlendeEnhetConsumer(
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client,
    private val metrikk: Metrikk,
) {
    private val logger = this.logger()
    private val sikkerlogger = sikkerLogger()

    fun hentBehandlendeEnhet(
        fnr: String,
        uuid: String,
    ): String {
        val geografiskTilknytning = hentGeografiskTilknytning(fnr)

        val criteria =
            ArbeidsfordelingRequest(
                tema = SYKEPENGER,
                diskresjonskode = geografiskTilknytning.diskresjonskode,
                geografiskOmraade = geografiskTilknytning.geografiskTilknytning,
            )

        try {
            val arbeidsfordelinger =
                runBlocking {
                    norg2Client.hentAlleArbeidsfordelinger(criteria, MdcUtils.getCallId())
                }
            logger.info("Fant enheter: $arbeidsfordelinger")
            val behandlendeEnhet =
                finnAktivBehandlendeEnhet(
                    arbeidsfordelinger,
                    geografiskTilknytning.geografiskTilknytning,
                )
            if (SYKEPENGER_UTLAND == behandlendeEnhet) {
                metrikk.tellInntektsmeldingSykepengerUtland()
            }
            logger.info(
                "Fant geografiskTilknytning ${geografiskTilknytning.geografiskTilknytning} med behandlendeEnhet $behandlendeEnhet for inntektsmelding $uuid",
            )
            return behandlendeEnhet
        } catch (e: RuntimeException) {
            "Klarte ikke å hente behandlende enhet!".also {
                logger.error(it)
                sikkerlogger.error(it, e)
            }
            throw BehandlendeEnhetFeiletException(e)
        }
    }

    fun hentGeografiskTilknytning(fnr: String): GeografiskTilknytningData {
        runBlocking {
            pdlClient.fullPerson(fnr)
        }.let {
            return GeografiskTilknytningData(
                geografiskTilknytning = it?.geografiskTilknytning,
                diskresjonskode = it?.diskresjonskode,
            )
        }
    }

    fun gjelderUtland(oppgave: UtsattOppgaveEntitet): Boolean {
        val behandlendeEnhet = this.hentBehandlendeEnhet(oppgave.fnr, oppgave.inntektsmeldingId)
        logger.info("Fant enhet $behandlendeEnhet for ${oppgave.arkivreferanse}")
        return (SYKEPENGER_UTLAND == behandlendeEnhet)
    }
}

fun finnAktivBehandlendeEnhet(
    arbeidsfordelinger: List<ArbeidsfordelingResponse>,
    geografiskTilknytning: String?,
): String =
    arbeidsfordelinger
        .stream()
        .findFirst()
        .orElseThrow { IngenAktivEnhetException(geografiskTilknytning, null) }
        .enhetNr!!
