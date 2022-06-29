@file:Suppress("UnstableApiUsage")

package no.nav.syfo.behandling

import com.google.common.util.concurrent.Striped
import log
import no.nav.syfo.client.aktor.AktorClient
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.validerInntektsmelding
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import java.time.LocalDateTime

class InntektsmeldingBehandler(
    private val journalpostService: JournalpostService,
    private val metrikk: Metrikk,
    private val inntektsmeldingService: InntektsmeldingService,
    private val aktorClient: AktorClient,
    private val inntektsmeldingAivenProducer: InntektsmeldingAivenProducer,
    private val utsattOppgaveService: UtsattOppgaveService
) {

    private val consumerLocks = Striped.lock(8)
    private val OPPRETT_OPPGAVE_FORSINKELSE = 48L
    private val log = log()

    fun behandle(arkivId: String, arkivreferanse: String): String? {
        log.info("Henter inntektsmelding for $arkivreferanse")
        val inntektsmelding = journalpostService.hentInntektsmelding(arkivId, arkivreferanse)
        if (JournalStatus.MOTTATT == inntektsmelding.journalStatus) {
            return behandleInntektsmelding(inntektsmelding)
        } else {
            log.info("Behandler ikke ${inntektsmelding.journalpostId} med status ${inntektsmelding.journalStatus}")
            return null
        }
    }

    fun behandleInntektsmelding(inntektsmelding: Inntektsmelding): String? {
        val consumerLock = consumerLocks.get(inntektsmelding.fnr)
        try {
            consumerLock.lock()
            log.info("Slår opp aktørID for ${inntektsmelding.arkivRefereranse}")
            val aktorid = aktorClient.getAktorId(inntektsmelding.fnr)
            log.info("Fant aktørid for ${inntektsmelding.arkivRefereranse}")
            inntektsmelding.aktorId = aktorid
            if (inntektsmeldingService.isDuplicate(inntektsmelding)) {
                behandleDuplikat(inntektsmelding)
            } else {
                return behandleVanlig(inntektsmelding)
            }
        } finally {
            consumerLock.unlock()
        }
        return null
    }

    fun behandleVanlig(inntektsmelding: Inntektsmelding): String {
        log.info("Likhetssjekk: ingen like detaljer fra før for ${inntektsmelding.arkivRefereranse}")
        metrikk.tellInntektsmeldingerMottatt(inntektsmelding)
        journalpostService.ferdigstillJournalpost(inntektsmelding)
        log.info("Ferdigstilte ${inntektsmelding.arkivRefereranse}")
        val dto = inntektsmeldingService.lagreBehandling(inntektsmelding, inntektsmelding.aktorId!!, inntektsmelding.arkivRefereranse)
        log.info("Lagret inntektsmelding ${inntektsmelding.arkivRefereranse}")
        utsattOppgaveService.opprett(
            UtsattOppgaveEntitet(
                fnr = inntektsmelding.fnr,
                aktørId = dto.aktorId,
                journalpostId = inntektsmelding.journalpostId,
                arkivreferanse = inntektsmelding.arkivRefereranse,
                inntektsmeldingId = dto.uuid,
                tilstand = Tilstand.Utsatt,
                timeout = LocalDateTime.now().plusHours(OPPRETT_OPPGAVE_FORSINKELSE),
                gosysOppgaveId = null,
                oppdatert = null,
                speil = false,
                utbetalingBruker = false
            )
        )
        log.info("Lagrer UtsattOppgave i databasen for ${inntektsmelding.arkivRefereranse}")
        val mappedInntektsmelding = mapInntektsmeldingKontrakt(
            inntektsmelding,
            inntektsmelding.aktorId!!,
            validerInntektsmelding(inntektsmelding),
            inntektsmelding.arkivRefereranse,
            dto.uuid
        )
        inntektsmeldingAivenProducer.leggMottattInntektsmeldingPåTopics(mappedInntektsmelding)
        tellMetrikker(inntektsmelding)
        log.info("Inntektsmelding {} er journalført for {} refusjon {}", inntektsmelding.journalpostId, inntektsmelding.arkivRefereranse, inntektsmelding.refusjon.beloepPrMnd)
        return dto.uuid
    }

    fun behandleDuplikat(inntektsmelding: Inntektsmelding) {
        log.info("Likhetssjekk: finnes fra før ${inntektsmelding.arkivRefereranse} og blir feilregistrert")
        metrikk.tellFunksjonellLikhet()
        journalpostService.feilregistrerJournalpost(inntektsmelding)
        metrikk.tellInntektsmeldingerFeilregistrert()
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
