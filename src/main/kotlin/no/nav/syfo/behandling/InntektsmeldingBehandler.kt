package no.nav.syfo.behandling

import com.google.common.util.concurrent.Striped
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
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
import org.slf4j.Logger
import java.time.LocalDateTime

const val OPPRETT_OPPGAVE_FORSINKELSE = 48L

class InntektsmeldingBehandler(
    private val journalpostService: JournalpostService,
    private val metrikk: Metrikk,
    private val inntektsmeldingService: InntektsmeldingService,
    private val inntektsmeldingAivenProducer: InntektsmeldingAivenProducer,
    private val utsattOppgaveService: UtsattOppgaveService,
    private val pdlClient: PdlClient,
) {
    private val logger: Logger = this.logger()
    private val sikkerlogger = sikkerLogger()
    private val consumerLocks = Striped.lock(8)

    fun behandle(
        arkivId: String,
        arkivreferanse: String,
    ) {
        logger.info("Henter inntektsmelding for $arkivreferanse")
        val inntektsmelding = journalpostService.hentInntektsmelding(arkivId, arkivreferanse)
        behandleInntektsmelding(arkivreferanse, inntektsmelding)
    }

    private fun behandleInntektsmelding(
        arkivreferanse: String,
        inntektsmelding: Inntektsmelding,
    ) {
        if (statusSkalIgnoreres(inntektsmelding.journalStatus)) {
            logger.info(
                "Behandler ikke journalpostId {} da den har status: {}",
                inntektsmelding.journalpostId,
                inntektsmelding.journalStatus,
            )
            return
        }
        val consumerLock = consumerLocks.get(inntektsmelding.fnr)
        try {
            consumerLock.lock()
            sikkerlogger.info("Behandler: $inntektsmelding")
            logger.info("Slår opp aktørID for ${inntektsmelding.arkivRefereranse}")
            val aktorid =
                runBlocking {
                    pdlClient.hentAktoerID(inntektsmelding.fnr)
                }
            if (aktorid == null) {
                "Fant ikke aktøren for arkivreferansen: $arkivreferanse".also {
                    logger.error(it)
                    sikkerlogger.error(it)
                }
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
                            utbetalingBruker = false,
                        ),
                    )
                    logger.info("Lagrer UtsattOppgave i databasen for ${inntektsmelding.arkivRefereranse}")

                    val mappedInntektsmelding =
                        mapInntektsmeldingKontrakt(
                            inntektsmelding,
                            aktorid,
                            validerInntektsmelding(inntektsmelding),
                            arkivreferanse,
                            dto.uuid,
                        )

                    inntektsmeldingAivenProducer.sendTilTopicForVedtaksloesning(mappedInntektsmelding)
                    inntektsmeldingAivenProducer.sendTilTopicForBruker(mappedInntektsmelding)
                    tellMetrikker(inntektsmelding)
                    logger.info(
                        "Inntektsmelding {} er journalført for {} refusjon {}",
                        inntektsmelding.journalpostId,
                        arkivreferanse,
                        inntektsmelding.refusjon.beloepPrMnd,
                    )
                }
            }
        } finally {
            consumerLock.unlock()
        }
    }

    private fun statusSkalIgnoreres(journalStatus: JournalStatus): Boolean {
        // kan vurdere å bare sjekke om status != MOTTATT..
        return journalStatus in listOf(JournalStatus.JOURNALFOERT, JournalStatus.FEILREGISTRERT, JournalStatus.UKJENT)
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
