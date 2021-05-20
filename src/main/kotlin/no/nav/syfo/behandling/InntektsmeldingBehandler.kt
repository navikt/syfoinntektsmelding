@file:Suppress("UnstableApiUsage")

package no.nav.syfo.behandling

import com.google.common.util.concurrent.Striped
import log
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.validerInntektsmelding
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import java.time.LocalDateTime

class InntektsmeldingBehandler(
    private val journalpostService: JournalpostService,
    private val saksbehandlingService: SaksbehandlingService,
    private val metrikk: Metrikk,
    private val inntektsmeldingService: InntektsmeldingService,
    private val aktorConsumer: AktorConsumer,
    private val inntektsmeldingProducer: InntektsmeldingProducer,
    private val utsattOppgaveService: UtsattOppgaveService
) {

    private val consumerLocks = Striped.lock(8)
    private val OPPRETT_OPPGAVE_FORSINKELSE = 48L;

    fun behandle(arkivId: String, arkivreferanse: String): String? {
        val inntektsmelding = journalpostService.hentInntektsmelding(arkivId, arkivreferanse)
        return behandle(arkivId, arkivreferanse, inntektsmelding)
    }

    fun behandle(arkivId: String, arkivreferanse: String, inntektsmelding: Inntektsmelding): String? {

        val log = log()
        var ret : String? = null
        val consumerLock = consumerLocks.get(inntektsmelding.fnr)
        try {
            consumerLock.lock()
            log.info("Slår opp aktørID for ${inntektsmelding.arkivRefereranse}")
            val aktorid = aktorConsumer.getAktorId(inntektsmelding.fnr)
            log.info("fant aktørid for ${inntektsmelding.arkivRefereranse}")

            tellMetrikker(inntektsmelding)

            if (JournalStatus.MIDLERTIDIG == inntektsmelding.journalStatus) {
                metrikk.tellInntektsmeldingerMottatt(inntektsmelding)

                val saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding, aktorid, arkivreferanse)
                log.info("fant sak $saksId")

                journalpostService.ferdigstillJournalpost(saksId, inntektsmelding)
                log.info("ferdigstilte ${inntektsmelding.arkivRefereranse}")

                val dto = inntektsmeldingService.lagreBehandling(inntektsmelding, aktorid, saksId, arkivreferanse)
                log.info("lagret ${inntektsmelding.arkivRefereranse}")

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
                log.info("opprettet utsatt oppgave på ${inntektsmelding.arkivRefereranse}")

                inntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(
                    mapInntektsmeldingKontrakt(
                        inntektsmelding,
                        aktorid,
                        validerInntektsmelding(inntektsmelding),
                        arkivreferanse,
                        dto.uuid
                    )
                )

                log.info("Inntektsmelding {} er journalført for {} refusjon {}", inntektsmelding.journalpostId, arkivreferanse, inntektsmelding.refusjon.beloepPrMnd)
                ret = dto.uuid
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
        return ret
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

