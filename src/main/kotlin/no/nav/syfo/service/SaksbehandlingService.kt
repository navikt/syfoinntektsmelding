package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer
import no.nav.syfo.domain.InntektsmeldingMeta
import no.nav.syfo.domain.Oppgave
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.repository.InntektsmeldingDAO
import no.nav.syfo.util.DateUtil
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.sammenslattPeriode
import org.springframework.stereotype.Service
import java.time.LocalDate

@io.ktor.util.KtorExperimentalAPI
@Service
class SaksbehandlingService(
        private val oppgaveClient: OppgaveClient,
        private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
        private val eksisterendeSakService: EksisterendeSakService,
        private val inntektsmeldingDAO: InntektsmeldingDAO,
        private val sakClient: SakClient,
        private val metrikk: Metrikk
) {

    val log = log()

    private fun helper(perioder: List<Periode>, periode: Periode): Boolean {
        return perioder.stream()
            .anyMatch { p -> DateUtil.overlapperPerioder(p, periode) }
    }

    private fun finnTilhorendeInntektsmelding(inntektsmelding: Inntektsmelding, aktorId: String): InntektsmeldingMeta? {
        return inntektsmeldingDAO.finnBehandledeInntektsmeldinger(aktorId)
            .firstOrNull { im ->
                im.arbeidsgiverperioder
                    .stream()
                    .anyMatch { p -> helper(inntektsmelding.arbeidsgiverperioder, p) }
            }
    }

    fun behandleInntektsmelding(inntektsmelding: Inntektsmelding, aktorId: String): String {

        val tilhorendeInntektsmelding = finnTilhorendeInntektsmelding(inntektsmelding, aktorId)
            ?.apply {
                log.info("Fant overlappende inntektsmelding, bruker samme saksId: {}", this.sakId)
                metrikk.tellOverlappendeInntektsmelding()
            }

        val sammenslattPeriode = sammenslattPeriode(inntektsmelding.arbeidsgiverperioder)

//        TODO Endre timestamp til arkivReferanse
        val msgId = "${inntektsmelding.id}" // inntektsmelding.arkivReferanse
        val saksId = finnSaksId(tilhorendeInntektsmelding, inntektsmelding, aktorId, sammenslattPeriode, msgId)

        opprettOppgave(inntektsmelding.fnr, aktorId, saksId, inntektsmelding.journalpostId)

        return saksId
    }

    private fun finnSaksId(tilhorendeInntektsmelding: InntektsmeldingMeta?, inntektsmelding: Inntektsmelding, aktorId: String, sammenslattPeriode: Periode?, msgId: String): String {
        return (tilhorendeInntektsmelding
                ?.sakId
                ?: inntektsmelding.arbeidsgiverOrgnummer
                        ?.let { eksisterendeSakService.finnEksisterendeSak(aktorId, sammenslattPeriode?.fom, sammenslattPeriode?.tom) }
                ?: opprettSak(aktorId, msgId))
    }

    @io.ktor.util.KtorExperimentalAPI
    private fun opprettSak(aktorId: String, msgId: String): String {
        var saksId = "";
        runBlocking {
            saksId = sakClient.opprettSak(aktorId, msgId).id.toString()
        }
        return saksId
    }

    @io.ktor.util.KtorExperimentalAPI
    private fun opprettOppgave(fnr: String, aktorId: String, saksId: String, journalpostId: String) {
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(fnr)
        val gjelderUtland = ("4474" == behandlendeEnhet)
        runBlocking {
        oppgaveClient.opprettOppgave(
            sakId = saksId,
            journalpostId = journalpostId,
            tildeltEnhetsnr =  behandlendeEnhet,
            aktoerId = aktorId,
            gjelderUtland = gjelderUtland
            )
        }
    }
}
