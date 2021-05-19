package no.nav.syfo.behandling

import com.google.common.util.concurrent.Striped
import log
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.validerInntektsmelding
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InntektsmeldingBehandler(
    private val journalpostService: JournalpostService,
    private val saksbehandlingService: SaksbehandlingService,
    private val metrikk: Metrikk,
    private val inntektsmeldingService: InntektsmeldingService,
    private val aktorConsumer: AktorConsumer,
    private val inntektsmeldingProducer: InntektsmeldingProducer,
    private val aivenIMProducer: InntektsmeldingAivenProducer,
    private val utsattOppgaveService: UtsattOppgaveService
) {

    val consumerLocks = Striped.lock(8)
    val OPPRETT_OPPGAVE_FORSINKELSE = 48L;

    fun behandle(arkivId: String, arkivreferanse: String): String? {
        val inntektsmelding = journalpostService.hentInntektsmelding(arkivId, arkivreferanse)
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

                utsattOppgaveService.opprett(
                    UtsattOppgaveEntitet(
                        fnr = inntektsmelding.fnr,
                        sakId = saksId,
                        aktørId = dto.aktorId,
                        journalpostId = inntektsmelding.journalpostId,
                        arkivreferanse = inntektsmelding.arkivRefereranse,
                        inntektsmeldingId = dto.uuid,
                        tilstand = Tilstand.Utsatt,
                        timeout = LocalDateTime.now().plusHours(OPPRETT_OPPGAVE_FORSINKELSE)
                    )
                )

                val mappedIM = mapInntektsmeldingKontrakt(
                    inntektsmelding,
                    aktorid,
                    validerInntektsmelding(inntektsmelding),
                    arkivreferanse,
                    dto.uuid
                )

                inntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(mappedIM)
                aivenIMProducer.leggMottattInntektsmeldingPåTopics(mappedIM)

                log.info("Inntektsmelding {} er journalført for {} refusjon {}", inntektsmelding.journalpostId, arkivreferanse, inntektsmelding.refusjon.beloepPrMnd)
                return dto.uuid
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

