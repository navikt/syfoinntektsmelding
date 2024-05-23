package no.nav.syfo.simba

import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.utils.logger
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.JournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.behandling.FantIkkeAktørException
import no.nav.syfo.behandling.OPPRETT_OPPGAVE_FORSINKELSE
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.util.getAktørid
import no.nav.syfo.util.validerInntektsmelding
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.time.LocalDateTime

class InntektsmeldingConsumer(
    props: Map<String, Any>,
    topicName: String,
    private val inntektsmeldingService: InntektsmeldingService,
    private val inntektsmeldingAivenProducer: InntektsmeldingAivenProducer,
    private val utsattOppgaveService: UtsattOppgaveService,
    private val pdlClient: PdlClient,
    private val skalKasteExceptionVedPDLFeil: Boolean = true
) : ReadynessComponent, LivenessComponent {

    private val consumer = KafkaConsumer<String, String>(props)
    private var ready = false
    private var error = false
    private val log = logger()
    private val sikkerlogger = sikkerLogger()

    init {
        log.info("Lytter på topic $topicName")
        consumer.subscribe(listOf(topicName))
    }

    private fun setIsReady(ready: Boolean) {
        this.ready = ready
    }

    private fun setIsError(isError: Boolean) {
        this.error = isError
    }

    fun start() {
        sikkerlogger.info("Starter InntektsmeldingConsumer...")
        consumer.use {
            setIsReady(true)
            while (!error) {
                it
                    .poll(Duration.ofMillis(100))
                    .forEach { record ->
                        try {
                            behandleRecord(record)
                            it.commitSync()
                        } catch (e: Throwable) {
                            sikkerlogger.error("InntektsmeldingConsumer: Klarte ikke behandle: ${record.value()}", e)
                            log.error("InntektsmeldingConsumer: Klarte ikke behandle hendelse. Stopper lytting!", e)
                            setIsError(true)
                        }
                    }
            }
        }
    }

    private fun behandleRecord(
        record: ConsumerRecord<String, String>,
    ) {
        val value = record.value()
        sikkerlogger.info("InntektsmeldingConsumer: Mottar record fra Simba: $value")
        val meldingFraSimba = value.fromJson(JournalfoertInntektsmelding.serializer())

        val im = inntektsmeldingService.findByJournalpost(meldingFraSimba.journalpostId)
        if (im != null) {
            sikkerlogger.info("InntektsmeldingConsumer: Behandler ikke ${meldingFraSimba.journalpostId}. Finnes allerede.")
        } else {
            behandle(meldingFraSimba.journalpostId, meldingFraSimba.inntektsmelding, meldingFraSimba.selvbestemt)
            log.info("InntektsmeldingConsumer: Behandlet inntektsmelding med journalpostId: ${meldingFraSimba.journalpostId}")
        }
    }

    private fun behandle(journalpostId: String, inntektsmeldingFraSimba: Inntektsmelding, selvbestemt: Boolean) {
        val aktorid = hentAktoeridFraPDL(inntektsmeldingFraSimba.identitetsnummer)
        val arkivreferanse = "im_$journalpostId"
        val inntektsmelding = mapInntektsmelding(arkivreferanse, aktorid, journalpostId, inntektsmeldingFraSimba, selvbestemt)
        val dto = inntektsmeldingService.lagreBehandling(inntektsmelding, aktorid)
        val timeout = if (selvbestemt) {
            sikkerlogger.info("Mottok selvbestemtIM med journalpostId $journalpostId, oppretter gosys-oppgave umiddelbart")
            LocalDateTime.now()
        } else {
            LocalDateTime.now().plusHours(OPPRETT_OPPGAVE_FORSINKELSE)
        }
        utsattOppgaveService.opprett(
            UtsattOppgaveEntitet(
                fnr = inntektsmelding.fnr,
                aktørId = dto.aktorId,
                journalpostId = inntektsmelding.journalpostId,
                arkivreferanse = inntektsmelding.arkivRefereranse,
                inntektsmeldingId = dto.uuid,
                tilstand = Tilstand.Utsatt,
                timeout = timeout,
                gosysOppgaveId = null,
                oppdatert = null,
                speil = false,
                utbetalingBruker = false
            )
        )
        val matcherSpleis = !selvbestemt // senere kan dette bli flere tilfeller

        val mappedInntektsmelding = mapInntektsmeldingKontrakt(
            inntektsmelding,
            aktorid,
            validerInntektsmelding(inntektsmelding),
            arkivreferanse,
            dto.uuid,
            matcherSpleis
        )

        inntektsmeldingAivenProducer.leggMottattInntektsmeldingPåTopics(mappedInntektsmelding)

        sikkerlogger.info("Publiserte inntektsmelding på topic: $mappedInntektsmelding")
    }

    private fun hentAktoeridFraPDL(identitetsnummer: String): String {
        val aktorid = pdlClient.getAktørid(identitetsnummer)
        if (aktorid == null) {
            sikkerlogger.error("Fant ikke aktøren for: $identitetsnummer")
            if (skalKasteExceptionVedPDLFeil) { // Oppslag feiler i dev-miljøer om ikke brukere er opprettet i PDL - og dev wipes rett som det er
                throw FantIkkeAktørException(null)
            }
        }
        return aktorid ?: "AktørID"
    }

    override suspend fun runReadynessCheck() {
        if (!ready) {
            throw IllegalStateException("Lytting på hendelser er ikke klar ennå")
        }
    }

    override suspend fun runLivenessCheck() {
        if (error) {
            throw IllegalStateException("Det har oppstått en feil og slutter å lytte på hendelser")
        }
    }
}
