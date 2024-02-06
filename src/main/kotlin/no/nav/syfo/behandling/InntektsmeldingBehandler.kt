@file:Suppress("UnstableApiUsage")

package no.nav.syfo.behandling

import com.google.common.util.concurrent.Striped
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.getAktørid
import no.nav.syfo.util.validerInntektsmelding
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

const val OPPRETT_OPPGAVE_FORSINKELSE = 72L

class InntektsmeldingBehandler(
    private val journalpostService: JournalpostService,
    private val metrikk: Metrikk,
    private val inntektsmeldingService: InntektsmeldingService,
    private val inntektsmeldingAivenProducer: InntektsmeldingAivenProducer,
    private val utsattOppgaveService: UtsattOppgaveService,
    private val pdlClient: PdlClient
) {
    private val logger: Logger = this.logger()
    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    private val consumerLocks = Striped.lock(8)

    fun behandle(arkivId: String, arkivreferanse: String): String? {
        logger.info("Henter inntektsmelding for $arkivreferanse")
        val inntektsmelding = journalpostService.hentInntektsmelding(arkivId, arkivreferanse)
        return behandleInntektsmelding(arkivreferanse, inntektsmelding)
    }

    fun behandleInntektsmelding(arkivreferanse: String, inntektsmelding: Inntektsmelding): String? {
        var ret: String? = null
        val consumerLock = consumerLocks.get(inntektsmelding.fnr)
        try {
            consumerLock.lock()
            sikkerlogger.info("Behandler: $inntektsmelding")
            logger.info("Slår opp aktørID for ${inntektsmelding.arkivRefereranse}")
            val aktorid = pdlClient.getAktørid(inntektsmelding.fnr)
            if (aktorid == null) {
                logger.error("Fant ikke aktøren for arkivreferansen: $arkivreferanse")
                throw FantIkkeAktørException(null)
            }
            logger.info("Fant aktørid for ${inntektsmelding.arkivRefereranse}")
            inntektsmelding.aktorId = aktorid
            if (inntektsmeldingService.isDuplicate(inntektsmelding)) {
                metrikk.tellFunksjonellLikhet()
                logger.info("Likhetssjekk: finnes fra før ${inntektsmelding.arkivRefereranse} og blir feilregistrert")
                if (JournalStatus.MOTTATT == inntektsmelding.journalStatus) {
                    journalpostService.feilregistrerJournalpost(inntektsmelding)
                    metrikk.tellInntektsmeldingerFeilregistrert()
                }
            } else {
                logger.info("Likhetssjekk: ingen like detaljer fra før for ${inntektsmelding.arkivRefereranse}")
                if (JournalStatus.MOTTATT == inntektsmelding.journalStatus) {
                    metrikk.tellInntektsmeldingerMottatt(inntektsmelding)

                    journalpostService.ferdigstillJournalpost(inntektsmelding)
                    logger.info("Ferdigstilte ${inntektsmelding.arkivRefereranse}")

                    val dto = inntektsmeldingService.lagreBehandling(inntektsmelding, aktorid)
                    logger.info("Lagret inntektsmelding ${inntektsmelding.arkivRefereranse}")

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
                    logger.info("Lagrer UtsattOppgave i databasen for ${inntektsmelding.arkivRefereranse}")

                    val mappedInntektsmelding = mapInntektsmeldingKontrakt(
                        inntektsmelding,
                        aktorid,
                        validerInntektsmelding(inntektsmelding),
                        arkivreferanse,
                        dto.uuid
                    )

                    inntektsmeldingAivenProducer.leggMottattInntektsmeldingPåTopics(mappedInntektsmelding)
                    tellMetrikker(inntektsmelding)
                    logger.info(
                        "Inntektsmelding {} er journalført for {} refusjon {}",
                        inntektsmelding.journalpostId,
                        arkivreferanse,
                        inntektsmelding.refusjon.beloepPrMnd
                    )
                    ret = dto.uuid
                } else {
                    logger.info(
                        "Behandler ikke inntektsmelding {} da den har status: {}",
                        inntektsmelding.journalpostId,
                        inntektsmelding.journalStatus
                    )
                }
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

        if (inntektsmelding.opphørAvNaturalYtelse.isEmpty()) {
            metrikk.tellNaturalytelse()
        }
    }
}
