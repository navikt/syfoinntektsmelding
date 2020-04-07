package no.nav.syfo.behandling

import com.google.common.util.concurrent.Striped
import log
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.FremtidigOppgave
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.OppgaveService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.validerInntektsmelding
import org.springframework.stereotype.Service
import java.util.*

@Service
class InntektsmeldingBehandler(
    private val journalpostService: JournalpostService,
    private val saksbehandlingService: SaksbehandlingService,
    private val metrikk: Metrikk,
    private val inntektsmeldingService: InntektsmeldingService,
    private val aktorConsumer: AktorConsumer,
    private val inntektsmeldingProducer: InntektsmeldingProducer,
    private val oppgaveService: OppgaveService
) {

    val consumerLocks = Striped.lock(8)

    fun behandle(arkivId: String, arkivreferanse: String): String? {
        val inntektsmelding = journalpostService.hentInntektsmelding(arkivId)
        return behandle(arkivId, arkivreferanse, inntektsmelding)
    }

    fun behandle(arkivId: String, arkivreferanse: String, inntektsmelding: Inntektsmelding): String? {

        val log = log()

        val consumerLock = consumerLocks.get(inntektsmelding.fnr)
        try {
            consumerLock.lock()
            val aktorid = aktorConsumer.getAktorId(inntektsmelding.fnr)

            tellMetrikker(inntektsmelding)

            if (JournalStatus.MIDLERTIDIG == inntektsmelding.journalStatus) {
                metrikk.tellInntektsmeldingerMottatt(inntektsmelding)

                val saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding, aktorid, arkivreferanse)

                journalpostService.ferdigstillJournalpost(saksId, inntektsmelding)

                val dto = inntektsmeldingService.lagreBehandling(inntektsmelding, aktorid, saksId, arkivreferanse)

                oppgaveService.planleggOppgave(
                    FremtidigOppgave(
                        fnr = inntektsmelding.fnr,
                        saksId = saksId,
                        aktørId = dto.aktorId,
                        journalpostId = inntektsmelding.journalpostId,
                        arkivreferanse = inntektsmelding.arkivRefereranse,
                        inntektsmeldingId = UUID.fromString(dto.uuid)
                    )
                )

                inntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(
                    mapInntektsmeldingKontrakt(
                        inntektsmelding,
                        aktorid,
                        validerInntektsmelding(inntektsmelding),
                        arkivreferanse,
                        dto.uuid!!
                    )
                )

                log.info("Inntektsmelding {} er journalført for {} refusjon {}", inntektsmelding.journalpostId, arkivreferanse, inntektsmelding.refusjon.beloepPrMnd)
                return dto.uuid!!
            } else {
                log.info(
                    "Behandler ikke inntektsmelding {} da den har status: {}",
                    inntektsmelding.journalpostId,
                    inntektsmelding.journalStatus
                )
            }
        } finally {
            consumerLock.unlock()
        }
        return null
    }

    private fun tellMetrikker(inntektsmelding: Inntektsmelding) {
        metrikk.tellJournalpoststatus(inntektsmelding.journalStatus)
        metrikk.tellInntektsmeldingerRedusertEllerIngenUtbetaling(inntektsmelding.begrunnelseRedusert)
        metrikk.tellKreverRefusjon(inntektsmelding.refusjon.beloepPrMnd?.toInt() ?: 0)
        metrikk.tellArbeidsgiverperioder(inntektsmelding.arbeidsgiverperioder.size.toString())

        if (inntektsmelding.opphørAvNaturalYtelse.isEmpty())
            metrikk.tellNaturalytelse()
    }

}

