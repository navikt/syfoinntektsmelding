package no.nav.syfo.behandling

import log
import com.google.common.util.concurrent.Striped
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
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
class InntektsmeldingBehandler (
        private val journalpostService: JournalpostService,
        private val saksbehandlingService: SaksbehandlingService,
        private val metrikk: Metrikk,
        private val inntektsmeldingService: InntektsmeldingService,
        private val aktorConsumer: AktorConsumer,
        private val inntektsmeldingProducer: InntektsmeldingProducer
) {

    val consumerLocks = Striped.lock(8)

    fun behandle(arkivId: String, arkivreferanse: String) : InntektsmeldingEntitet? {

        val log = log()

        val inntektsmelding = journalpostService.hentInntektsmelding(arkivId)
        val consumerLock = consumerLocks.get(inntektsmelding.fnr)
        try {
            consumerLock.lock()
            val aktorid = aktorConsumer.getAktorId(inntektsmelding.fnr)

            metrikk.tellJournalpoststatus(inntektsmelding.journalStatus);

            if (JournalStatus.MIDLERTIDIG == inntektsmelding.journalStatus) {
                metrikk.tellInntektsmeldingerMottatt(inntektsmelding)

                val saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding, aktorid, arkivreferanse)

                journalpostService.ferdigstillJournalpost(saksId, inntektsmelding)

                val dto = inntektsmeldingService.lagreBehandling(inntektsmelding, aktorid, saksId, arkivreferanse)

                inntektsmeldingProducer.leggMottattInntektsmeldingPåTopic(
                        mapInntektsmeldingKontrakt(
                                inntektsmelding,
                                aktorid,
                                validerInntektsmelding(inntektsmelding),
                                arkivreferanse,
                                dto.uuid!!
                        )
                )

                log.info("Inntektsmelding {} er journalført for {}", inntektsmelding.journalpostId, arkivreferanse)
                return dto
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

}

