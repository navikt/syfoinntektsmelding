package no.nav.syfo.behandling

import com.google.common.util.concurrent.Striped
import log
import no.nav.syfo.api.BehandlingException
import no.nav.syfo.api.Feiltype
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.validerInntektsmelding
import org.springframework.stereotype.Service

@Service
class InntektsmeldingBehandler(
    private val journalpostService: JournalpostService,
    private val saksbehandlingService: SaksbehandlingService,
    private val metrikk: Metrikk,
    private val inntektsmeldingService: InntektsmeldingService,
    private val aktorConsumer: AktorConsumer,
    private val inntektsmeldingProducer: InntektsmeldingProducer
) {

    val consumerLocks = Striped.lock(8)

    @Throws(BehandlingException::class)
    fun behandle(arkivId: String, arkivReferanse: String): String? {
        val inntektsmelding: Inntektsmelding
        try {
            inntektsmelding = journalpostService.hentInntektsmelding(arkivId)
        } catch (ex1: Exception) {
            metrikk.tellBehandlingsfeil(Feiltype.HENT_INNTEKTSMELDING_FRA_JOURNALPOST)
            throw BehandlingException(ex1)
        }
        return behandle(arkivId, arkivReferanse, inntektsmelding)
    }

    @Throws(BehandlingException::class)
    fun behandle(arkivId: String, arkivReferanse: String, inntektsmelding: Inntektsmelding): String? {

        val log = log()

        val consumerLock = consumerLocks.get(inntektsmelding.fnr)
        try {
            consumerLock.lock()
            val aktorid: String
            try {
                aktorid = aktorConsumer.getAktorId(inntektsmelding.fnr)
            } catch (ex1: Exception) {
                metrikk.tellBehandlingsfeil(Feiltype.HENT_AKTØR_ID)
                throw BehandlingException(ex1)
            }

            tellMetrikker(inntektsmelding)

            if (inntektsmelding.journalStatus == JournalStatus.MIDLERTIDIG) {
                metrikk.tellInntektsmeldingerMottatt(inntektsmelding)

                val saksId: String
                try {
                    saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding, aktorid, arkivReferanse)
                } catch (ex1: Exception) {
                    metrikk.tellBehandlingsfeil(Feiltype.OPPGAVE)
                    throw BehandlingException(ex1)
                }

                try {
                    journalpostService.ferdigstillJournalpost(saksId, inntektsmelding)
                } catch (ex1: Exception) {
                    metrikk.tellBehandlingsfeil(Feiltype.FERDIGSTILL_JOURNALPOST)
                    throw BehandlingException(ex1)
                }

                val dto: InntektsmeldingEntitet
                try {
                    dto = inntektsmeldingService.lagreBehandling(inntektsmelding, aktorid, saksId, arkivReferanse)
                } catch (ex1: Exception) {
                    metrikk.tellBehandlingsfeil(Feiltype.LAGRE_BEHANDLING)
                    throw BehandlingException(ex1)
                }

                try {
                    inntektsmeldingProducer.leggMottattInntektsmeldingPåTopic(
                        mapInntektsmeldingKontrakt(
                            inntektsmelding,
                            aktorid,
                            validerInntektsmelding(inntektsmelding),
                            arkivReferanse,
                            dto.uuid!!
                        )
                    )
                } catch (ex1: Exception) {
                    metrikk.tellBehandlingsfeil(Feiltype.KAFKA)
                    throw BehandlingException(ex1)
                }


                log.info("Inntektsmelding {} er journalført for {}", inntektsmelding.journalpostId, arkivReferanse)
                return dto.uuid!!
            } else {
                metrikk.tellIkkebehandlet()
                log.info(
                    "Behandler ikke inntektsmelding {} da den har status: {} med arkivreferanse $arkivReferanse",
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

