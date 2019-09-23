package no.nav.syfo.service

import log
import no.nav.syfo.consumer.ws.BehandleSakConsumer
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

@Service
class SaksbehandlingService(
    private val oppgavebehandlingConsumer: OppgavebehandlingConsumer,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val behandleSakConsumer: BehandleSakConsumer,
    private val eksisterendeSakService: EksisterendeSakService,
    private val inntektsmeldingDAO: InntektsmeldingDAO,
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

        val saksId = tilhorendeInntektsmelding
            ?.sakId
            ?: inntektsmelding.arbeidsgiverOrgnummer
                ?.let { eksisterendeSakService.finnEksisterendeSak(aktorId, sammenslattPeriode?.fom, sammenslattPeriode?.tom) }
            ?: behandleSakConsumer.opprettSak(inntektsmelding.fnr)


        opprettOppgave(inntektsmelding.fnr, byggOppgave(inntektsmelding.journalpostId, saksId))

        return saksId
    }

    private fun opprettOppgave(fnr: String, oppgave: Oppgave) {
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(fnr)
        val geografiskTilknytning = behandlendeEnhetConsumer.hentGeografiskTilknytning(fnr)

        val nyOppgave = Oppgave(
            aktivTil = oppgave.aktivTil,
            beskrivelse = oppgave.beskrivelse,
            saksnummer = oppgave.saksnummer,
            dokumentId = oppgave.dokumentId,
            geografiskTilknytning = geografiskTilknytning.geografiskTilknytning,
            ansvarligEnhetId = behandlendeEnhet
        )

        oppgavebehandlingConsumer.opprettOppgave(fnr, nyOppgave)
    }

    private fun byggOppgave(dokumentId: String, saksnummer: String): Oppgave {
        return Oppgave(
            dokumentId = dokumentId,
            saksnummer= saksnummer,
            beskrivelse= "Det har kommet en inntektsmelding på sykepenger.",
            aktivTil= LocalDate.now().plusDays(7)
        )
    }
}
