package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.client.SakClient
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.DateUtil
import no.nav.syfo.util.Metrikk

class SaksbehandlingService(
    private val inntektsmeldingService: InntektsmeldingService,
    private val sakClient: SakClient,
    private val metrikk: Metrikk
) {

    val log = log()

    private fun harOverlappendePerioder(perioder: List<Periode>, periode: Periode): Boolean {
        return perioder.stream()
            .anyMatch { p -> DateUtil.overlapperPerioder(p, periode) }
    }

    /**
     * Finner første inntektsmelding for aktørId som matcher arbeidsgiverperiodene
     */
    private fun finnInntektsmeldingMedArbeidsgiverperioder(arbeidsgiverperioder: List<Periode>, aktorId: String): Inntektsmelding? {
        return inntektsmeldingService.finnBehandledeInntektsmeldinger(aktorId)
            .firstOrNull { im ->
                im.arbeidsgiverperioder.any { p ->
                    harOverlappendePerioder(arbeidsgiverperioder, p)
                }
            }
    }

    /**
     * Returnerer saksId
     */
    fun finnEllerOpprettSakForInntektsmelding(inntektsmelding: Inntektsmelding, aktorId: String, arkivReferanse: String): String {
        val tilhorendeInntektsmelding = finnInntektsmeldingMedArbeidsgiverperioder(inntektsmelding.arbeidsgiverperioder, aktorId)?.apply {
            log.info("Fant overlappende inntektsmelding, bruker samme saksId: {}", this.sakId)
            metrikk.tellOverlappendeInntektsmelding()
        }
        if (tilhorendeInntektsmelding?.sakId.isNullOrEmpty()) {
            metrikk.tellInntektsmeldingNySak()
            return opprettSak(aktorId, arkivReferanse)
        } else {
            metrikk.tellInntektsmeldingSaksIdFraDB()
            return tilhorendeInntektsmelding?.sakId!!
        }
    }

    private fun opprettSak(aktorId: String, msgId: String): String {
        return runBlocking {
            sakClient.opprettSak(aktorId, msgId).id.toString()
        }
    }
}
