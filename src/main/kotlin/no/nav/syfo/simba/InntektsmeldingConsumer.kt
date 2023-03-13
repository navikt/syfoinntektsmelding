package no.nav.syfo.simba

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.InntektsmeldingDokument
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.JournalførtInntektsmelding
import no.nav.syfo.behandling.OPPRETT_OPPGAVE_FORSINKELSE
import no.nav.syfo.client.aktor.AktorClient
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.util.validerInntektsmelding
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

class InntektsmeldingConsumer(
    props: Map<String, Any>,
    topicName: String,
    val om: ObjectMapper,
    val inntektsmeldingService: InntektsmeldingService,
    val inntektsmeldingAivenProducer: InntektsmeldingAivenProducer,
    val utsattOppgaveService: UtsattOppgaveService,
    val aktorClient: AktorClient
) : ReadynessComponent, LivenessComponent {

    private val consumer = KafkaConsumer<String, String>(props)
    private var ready = false
    private var error = false
    private val log = LoggerFactory.getLogger(InntektsmeldingConsumer::class.java)
    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")

    init {
        log.info("Lytter på topic $topicName")
        consumer.subscribe(listOf(topicName))
    }

    fun setIsReady(ready: Boolean) {
        this.ready = ready
    }

    fun setIsError(isError: Boolean) {
        this.error = isError
    }

    fun start() {
        sikkerlogger.info("Starter InntektsmeldingConsumer...")
        consumer.use {
            setIsReady(true)
            while (!error) {
                it
                    .poll(Duration.ofMillis(1000))
                    .forEach { record ->
                        try {
                            val value = record.value()
                            sikkerlogger.info("InntektsmeldingConsumer: Mottar record: $value")
                            val jd = om.readValue<JournalførtInntektsmelding>(value, JournalførtInntektsmelding::class.java)
                            behandle(jd.journalpostId, jd.inntektsmeldingDokument)
                            log.info("InntektsmeldingConsumer: Behandlet inntektsmelding med journalpostid: ${jd.journalpostId}")
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

    fun behandle(journalpostId: String, inntektsmeldingDokument: InntektsmeldingDokument) {
        val aktorid = aktorClient.getAktorId(inntektsmeldingDokument.identitetsnummer)
        val arkivreferanse = ""
        val inntektsmelding = mapInntektsmelding(arkivreferanse, aktorid, journalpostId, inntektsmeldingDokument)
        val dto = inntektsmeldingService.lagreBehandling(inntektsmelding, aktorid, arkivreferanse)

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

        val mappedInntektsmelding = mapInntektsmeldingKontrakt(
            inntektsmelding,
            aktorid,
            validerInntektsmelding(inntektsmelding),
            arkivreferanse,
            dto.uuid
        )

        inntektsmeldingAivenProducer.leggMottattInntektsmeldingPåTopics(mappedInntektsmelding)

        log.info("Publiserte inntektsmelding på topic: $inntektsmeldingDokument")
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
