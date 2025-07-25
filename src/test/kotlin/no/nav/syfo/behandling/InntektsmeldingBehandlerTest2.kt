package no.nav.syfo.behandling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.client.saf.model.Dokument
import no.nav.syfo.client.saf.model.Journalpost
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingRepositoryMock
import no.nav.syfo.service.BehandleInngaaendeJournalConsumer
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.InngaaendeJournalConsumer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.service.JournalConsumer
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.inntektsmeldingArbeidsgiver
import no.nav.syfo.service.inntektsmeldingArbeidsgiverPrivat
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.getAktørid
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class InntektsmeldingBehandlerTest2 {
    private var objectMapper =
        ObjectMapper()
            .registerModule(
                KotlinModule
                    .Builder()
                    .withReflectionCacheSize(512)
                    .configure(KotlinFeature.NullToEmptyCollection, false)
                    .configure(KotlinFeature.NullToEmptyMap, false)
                    .configure(KotlinFeature.NullIsSameAsDefault, false)
                    .configure(KotlinFeature.SingletonSupport, false)
                    .configure(KotlinFeature.StrictNullChecks, false)
                    .build(),
            ).registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private var inngaaendeJournalConsumer = mockk<InngaaendeJournalConsumer>(relaxed = true)
    private var metrikk = mockk<Metrikk>(relaxed = true)
    private var behandleInngaaendeJournalConsumer = mockk<BehandleInngaaendeJournalConsumer>(relaxed = true)
    private var behandlendeEnhetConsumer = mockk<BehandlendeEnhetConsumer>(relaxed = true)
    private var journalpostService = mockk<JournalpostService>(relaxed = true)

    private var inntektsmeldingRepository = mockk<InntektsmeldingRepository>(relaxed = true)
    private var mockInnteksmeldingRepo = InntektsmeldingRepositoryMock()
    private var inntektsmeldingService =
        InntektsmeldingService(
            mockInnteksmeldingRepo,
            objectMapper,
        ) // mockk<InntektsmeldingService>(relaxed = true)

    private var utsattOppgaveService = mockk<UtsattOppgaveService>(relaxed = true)

    private var journalConsumer = mockk<JournalConsumer>(relaxed = true)
    private var safDokumentClient = mockk<SafDokumentClient>(relaxed = true)
    private var safJournalpostClient = mockk<SafJournalpostClient>(relaxed = true)
    private var inntektsmeldingBehandler = mockk<InntektsmeldingBehandler>(relaxed = true)
    private var aivenInntektsmeldingBehandler = mockk<InntektsmeldingAivenProducer>(relaxed = true)
    private val pdlClient = mockk<PdlClient>(relaxed = true)

    @BeforeEach
    fun setup() {
        inntektsmeldingRepository.deleteAll()
        journalConsumer = JournalConsumer(safDokumentClient, safJournalpostClient, pdlClient)
        journalpostService =
            JournalpostService(
                inngaaendeJournalConsumer,
                behandleInngaaendeJournalConsumer,
                journalConsumer,
                behandlendeEnhetConsumer,
                metrikk,
            )

        inntektsmeldingBehandler =
            InntektsmeldingBehandler(
                journalpostService,
                metrikk,
                inntektsmeldingService,
                aivenInntektsmeldingBehandler,
                utsattOppgaveService,
                pdlClient,
            )
        MockKAnnotations.init(inntektsmeldingBehandler)

        every { inngaaendeJournalConsumer.hentDokumentId("arkivId") } returns inngaaendeJournal("arkivId")

        every { pdlClient.getAktørid(any()) } answers { "aktorId_for_" + firstArg() }

        every { behandlendeEnhetConsumer.hentBehandlendeEnhet(any(), any()) } returns "enhet"
        every { behandlendeEnhetConsumer.hentGeografiskTilknytning(any()) } returns
            GeografiskTilknytningData(geografiskTilknytning = "tilknytning", diskresjonskode = "")
        val journalpost =
            Journalpost(
                JournalStatus.MOTTATT,
                LocalDateTime.now(),
                dokumenter = listOf(Dokument(dokumentInfoId = "dokumentId")),
            )
        every {
            safJournalpostClient.getJournalpostMetadata(any())
        } returns journalpost
    }

    @Test
    fun `Gjenbruker saksId hvis vi får to overlappende inntektsmeldinger`() {
        every { pdlClient.getAktørid(any()) } returnsMany listOf("aktorId_for_1", "aktorId_for_1")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId1") } returns inngaaendeJournal("arkivId1")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId2") } returns inngaaendeJournal("arkivId2")
        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 16))
        every {
            safDokumentClient.hentDokument(any(), any())
        } returnsMany listOf(dokumentResponse1.toByteArray(), dokumentResponse2.toByteArray())
        inntektsmeldingBehandler.behandle("arkivId1", "AR-1")
        inntektsmeldingBehandler.behandle("arkivId2", "AR-2")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_1")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo(inntektsmeldingMetas[1].sakId)
    }

    @Test
    fun `Gjenbruker ikke saksId hvis vi får to inntektsmeldinger som ikke overlapper`() {
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId3") } returns inngaaendeJournal("arkivId3")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId4") } returns inngaaendeJournal("arkivId4")

        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16))

        every {
            safDokumentClient.hentDokument(any(), any())
        } returnsMany listOf(dokumentResponse1.toByteArray(), dokumentResponse2.toByteArray())

        every { pdlClient.getAktørid(any()) } returnsMany listOf("778", "778")

        inntektsmeldingBehandler.behandle("arkivId3", "AR-3")
        inntektsmeldingBehandler.behandle("arkivId4", "AR-4")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("778")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        Assertions.assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("988")
    }

    @Test
    fun `Mottar inntektsmelding uten arbeidsgiverperioder`() {
        every { pdlClient.getAktørid(any()) } returnsMany listOf("aktorId_for_7", "aktorId_for_7")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId7") } returns inngaaendeJournal("arkivId7")

        every {
            safDokumentClient.hentDokument("arkivId7", any())
        } returns lagDokumentRespons().toByteArray()

        inntektsmeldingBehandler.behandle("arkivId7", "AR-7")
        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_7")
        Assertions.assertThat(inntektsmeldinger.size).isEqualTo(1)
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    fun `Mottar inntektsmelding med flere perioder`() {
        every { pdlClient.getAktørid(any()) } returnsMany listOf("aktorId_for_8", "aktorId_for_8")
        val dokumentResponse =
            lagDokumentRespons(
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 12),
                LocalDate.of(2019, 1, 12),
                LocalDate.of(2019, 1, 14),
            )
        every {
            safDokumentClient.hentDokument(any(), any())
        } returns dokumentResponse.toByteArray()

        every { inngaaendeJournalConsumer.hentDokumentId("arkivId_8") } returns inngaaendeJournal("arkivId_8")

        inntektsmeldingBehandler.behandle("arkivId_8", "AR-8")

        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_8")

        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019, 1, 1))
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019, 1, 12))
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[1].fom).isEqualTo(LocalDate.of(2019, 1, 12))
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[1].tom).isEqualTo(LocalDate.of(2019, 1, 14))
    }

    @Test
    fun `Mottar inntektsmelding med privat arbeidsgiver`() {
        every { pdlClient.getAktørid(any()) } returnsMany listOf("aktorId_for_9", "aktorId_for_9")
        every {
            safDokumentClient.hentDokument(any(), any())
        } returns inntektsmeldingArbeidsgiverPrivat().toByteArray()

        every { inngaaendeJournalConsumer.hentDokumentId("arkivId_9") } returns inngaaendeJournal("arkivId_9")

        inntektsmeldingBehandler.behandle("arkivId_9", "AR-9")

        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_9")

        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverPrivatFnr).isEqualTo("arbeidsgiverPrivat")
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverOrgnummer).isNull()
        Assertions.assertThat(inntektsmeldinger[0].aktorId).isEqualTo("aktorId_for_9")
    }

    @Test
    fun `Behandler inntektsmelding som en sak ved lik periode`() {
        val dokumentResponse =
            inntektsmeldingArbeidsgiver(
                listOf(
                    Periode(LocalDate.now(), LocalDate.now().plusDays(20)),
                ),
            )
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId") } returns inngaaendeJournal("arkivId")
        every {
            safDokumentClient.hentDokument("arkivId", any())
        } returns dokumentResponse.toByteArray()

        val numThreads = 16
        produceParallelMessages(numThreads, "arkivId")

        verify(exactly = numThreads) { behandleInngaaendeJournalConsumer.ferdigstillJournalpost(any()) }
    }

    @Test
    fun `Behandler inntektsmeldinger for flere personer samtidig`() =
        runBlocking {
            val fnr = AtomicInteger()
            every {
                safDokumentClient.hentDokument(any(), any())
            } answers { build(fnr).toByteArray() }

            val numThreads = 16
            produceParallelMessages(numThreads, "arkivId")
            verify(exactly = numThreads) { behandleInngaaendeJournalConsumer.ferdigstillJournalpost(any()) }
        }

    private fun build(fnr: AtomicInteger): String =
        inntektsmeldingArbeidsgiver(
            listOf(
                Periode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(20),
                ),
            ),
            "fnr" + fnr.incrementAndGet(),
        )

    fun produceParallelMessages(
        numThreads: Int,
        arkivId: String,
    ) {
        val countdown = CountDownLatch(numThreads)

        repeat(numThreads) {
            Thread {
                inntektsmeldingBehandler.behandle(arkivId, "ar-$numThreads")
                countdown.countDown()
            }.start()
        }
        countdown.await()
    }

    private fun lagDokumentRespons(
        fom: LocalDate,
        tom: LocalDate,
    ): String =
        inntektsmeldingArbeidsgiver(
            listOf(Periode(fom, tom)),
        )

    private fun lagDokumentRespons(): String =
        inntektsmeldingArbeidsgiver(
            ArrayList(),
        )

    private fun lagDokumentRespons(
        fom: LocalDate,
        tom: LocalDate,
        fom2: LocalDate,
        tom2: LocalDate,
    ): String =
        inntektsmeldingArbeidsgiver(
            listOf(Periode(fom, tom), Periode(fom2, tom2)),
        )

    private fun inngaaendeJournal(arkivId: String): InngaaendeJournal =
        InngaaendeJournal(
            dokumentId = arkivId,
            status = JournalStatus.MOTTATT,
            mottattDato = LocalDateTime.now(),
        )
}
