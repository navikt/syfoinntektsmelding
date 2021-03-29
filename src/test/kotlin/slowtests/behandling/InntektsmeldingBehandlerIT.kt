package slowtests.behandling

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.SakResponse
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.*
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.producer.InntektsmeldingProducer
import slowtests.repository.InntektsmeldingRepository
import slowtests.repository.InntektsmeldingService
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.context.TestPropertySource
//import org.springframework.test.context.junit4.SpringRunner
//import org.springframework.test.context.web.WebAppConfiguration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger



open class InntektsmeldingBehandlerIT {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    var journalV2 = mockk<JournalV2>(relaxed = true)
    var aktorConsumer= mockk<AktorConsumer>(relaxed = true)
    var inngaaendeJournalConsumer = mockk<InngaaendeJournalConsumer>(relaxed = true)
    var metrikk = mockk<Metrikk>(relaxed = true)
    var inntektsmeldingProducer = mockk<InntektsmeldingProducer>(relaxed = true)
    var behandleInngaaendeJournalConsumer = mockk<BehandleInngaaendeJournalConsumer>(relaxed = true)
    var behandlendeEnhetConsumer = mockk<BehandlendeEnhetConsumer>(relaxed = true)
    var oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    var sakClient = mockk<SakClient>(relaxed = true)
    var eksisterendeSakService = mockk<EksisterendeSakService>(relaxed = true)
    var journalpostService = mockk<JournalpostService>(relaxed = true)


    lateinit var inntektsmeldingRepository: InntektsmeldingRepository
    lateinit var inntektsmeldingService: InntektsmeldingService

    lateinit var utsattOppgaveDAO: UtsattOppgaveDAO
    lateinit var utsattOppgaveService: UtsattOppgaveService
    //lateinit var utsattOppgaveConsumer: UtsattOppgaveConsumer


    lateinit var journalConsumer: JournalConsumer
    lateinit var saksbehandlingService: SaksbehandlingService
    lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @Before
    fun setup() {
        inntektsmeldingRepository.deleteAll()
        journalConsumer = JournalConsumer(journalV2, aktorConsumer)
        journalpostService = JournalpostService(inngaaendeJournalConsumer, behandleInngaaendeJournalConsumer, journalConsumer, behandlendeEnhetConsumer, metrikk)
        saksbehandlingService = SaksbehandlingService(eksisterendeSakService, inntektsmeldingService, sakClient, metrikk)
        inntektsmeldingBehandler = InntektsmeldingBehandler(journalpostService, saksbehandlingService, metrikk, inntektsmeldingService, aktorConsumer, inntektsmeldingProducer, utsattOppgaveService)
        MockKAnnotations.init(inntektsmeldingBehandler)
        runBlocking {
            coEvery { sakClient.opprettSak(any(), any()) }  returnsMany listOf(
                SakResponse(id = 987, tema = "SYM", aktoerId = "444", applikasjon = "", fagsakNr = "123000", opprettetAv = "meg", opprettetTidspunkt = ZonedDateTime.now(), orgnr = "999888777"),
                SakResponse(id = 988, tema = "SYM", aktoerId = "444", applikasjon = "", fagsakNr = "123000", opprettetAv = "meg", opprettetTidspunkt = ZonedDateTime.now(), orgnr = "999888777")
            )
        }
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId") } returns inngaaendeJournal("arkivId")

        every { aktorConsumer.getAktorId(any()) } answers { "aktorId_for_" + firstArg() }

        every { eksisterendeSakService.finnEksisterendeSak(any(), any(), any()) } returns null
        every { behandlendeEnhetConsumer.hentBehandlendeEnhet(any(),any()) } returns "enhet"
        every { behandlendeEnhetConsumer.hentGeografiskTilknytning(any()) } returns
            GeografiskTilknytningData(geografiskTilknytning = "tilknytning", diskresjonskode = "")
        }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Gjenbruker saksId hvis vi får to overlappende inntektsmeldinger`() {
        every { aktorConsumer.getAktorId(any()) } returnsMany listOf("aktorId_for_1", "aktorId_for_1")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId1") } returns inngaaendeJournal("arkivId1")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId2") } returns inngaaendeJournal("arkivId2")
        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 16))
        every { journalV2.hentDokument(any()) } returnsMany listOf(dokumentResponse1.response , dokumentResponse2.response)
        inntektsmeldingBehandler.behandle("arkivId1", "AR-1")
        inntektsmeldingBehandler.behandle("arkivId2", "AR-2")
        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_1")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo(inntektsmeldingMetas[1].sakId)
        runBlocking {
            coVerify(exactly = 1) { sakClient.opprettSak(eq("aktorId_for_1"), any()) }
        }
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Gjenbruker ikke saksId hvis vi får to inntektsmeldinger som ikke overlapper`() {
         every { inngaaendeJournalConsumer.hentDokumentId("arkivId3") } returns inngaaendeJournal("arkivId3")
         every { inngaaendeJournalConsumer.hentDokumentId("arkivId4") } returns inngaaendeJournal("arkivId4")

        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16))

        every { journalV2.hentDokument(any()) } returnsMany listOf(
            dokumentResponse1.response,
            dokumentResponse2.response
        )

        every { aktorConsumer.getAktorId(any()) } returnsMany listOf("778", "778")

        inntektsmeldingBehandler.behandle("arkivId3", "AR-3")
        inntektsmeldingBehandler.behandle("arkivId4", "AR-4")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("778")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        Assertions.assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("988")

        runBlocking {
            coVerify(exactly = 2) { sakClient.opprettSak(eq("778"), any()) }
        }
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Bruker saksId fra sykeforløp om vi ikke har overlappende inntektsmelding`() {
        every { eksisterendeSakService.finnEksisterendeSak(any(), any(), any()) } returnsMany listOf(null, "syfosak")

        every { inngaaendeJournalConsumer.hentDokumentId("arkivId5") } returns inngaaendeJournal("arkivId5")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId6") } returns inngaaendeJournal("arkivId6")

        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16))

        every { journalV2.hentDokument(any()) } returnsMany listOf(
            dokumentResponse1.response,
            dokumentResponse2.response
        )
        every { aktorConsumer.getAktorId(any()) } returnsMany listOf("999", "999")

        inntektsmeldingBehandler.behandle("arkivId5", "AR-5")
        inntektsmeldingBehandler.behandle("arkivId6", "AR-6")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("999")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        Assertions.assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("syfosak")

        runBlocking {
            coVerify(exactly = 1) { sakClient.opprettSak(any(), any()) }
        }
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Mottar inntektsmelding uten arbeidsgiverperioder`() {
        every { aktorConsumer.getAktorId(any()) } returnsMany listOf("aktorId_for_7", "aktorId_for_7")
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId7") } returns inngaaendeJournal("arkivId7")
        val dokumentResponse = lagDokumentRespons()
        every { journalV2.hentDokument(any()) } returns dokumentResponse.response
        inntektsmeldingBehandler.behandle("arkivId7", "AR-7")
        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_7")
        Assertions.assertThat(inntektsmeldinger.size).isEqualTo(1)
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Mottar inntektsmelding med flere perioder`() {
        every { aktorConsumer.getAktorId(any()) } returnsMany listOf("aktorId_for_8", "aktorId_for_8")
        val dokumentResponse = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 14))
        every { journalV2.hentDokument(any()) } returns dokumentResponse.response
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
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Mottar inntektsmelding med privat arbeidsgiver`() {
        every { aktorConsumer.getAktorId(any()) } returnsMany listOf("aktorId_for_9", "aktorId_for_9")
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse.response = HentDokumentResponse()
        dokumentResponse.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiverPrivat().toByteArray()
        every { journalV2.hentDokument(any()) } returns dokumentResponse.response
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId_9") } returns inngaaendeJournal("arkivId_9")

        inntektsmeldingBehandler.behandle("arkivId_9", "AR-9")

        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_9")

        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverPrivatFnr).isEqualTo("arbeidsgiverPrivat")
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverOrgnummer).isNull()
        Assertions.assertThat(inntektsmeldinger[0].aktorId).isEqualTo("aktorId_for_9")
    }

    @Test
    @Throws(Exception::class)
    open fun `Behandler inntektsmelding som en sak ved lik periode`() {
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse.response = HentDokumentResponse()
        dokumentResponse.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            listOf(
                Periode(LocalDate.now(), LocalDate.now().plusDays(20))
            )
        ).toByteArray()
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId") } returns inngaaendeJournal("arkivId")
        every { journalV2.hentDokument(any()) } returns dokumentResponse.response

        val numThreads = 16
        produceParallelMessages(numThreads, "arkivId")

        runBlocking {
            coVerify(exactly = 1) { sakClient.opprettSak(any(), any()) }
        }
        verify(exactly = numThreads) { behandleInngaaendeJournalConsumer.ferdigstillJournalpost(any()) }
    }

    @Test
    @Throws(Exception::class)
    fun `Behandler inntektsmeldinger for flere personer samtidig`() = runBlocking {
        val fnr = AtomicInteger()
        every { journalV2.hentDokument(any()) } answers { build(fnr) }

        val numThreads = 16
        produceParallelMessages(numThreads, "arkivId")
        coVerify(exactly = numThreads) { sakClient.opprettSak(any(), any()) }
        verify(exactly = numThreads) { behandleInngaaendeJournalConsumer.ferdigstillJournalpost(any()) }
    }

    fun build(fnr: AtomicInteger): HentDokumentResponse {
        val dokumentResponse = HentDokumentResponse()
        dokumentResponse.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            listOf(
                Periode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(20)
                )
            ),
            "fnr" + fnr.incrementAndGet()
        ).toByteArray()
        return dokumentResponse
    }

    @Throws(Exception::class)
    fun produceParallelMessages(numThreads: Int, arkivId: String) {
        val countdown = CountDownLatch(numThreads)

        repeat(numThreads) {
            Thread {
                inntektsmeldingBehandler.behandle(arkivId, "ar-${numThreads.toString()}")
                countdown.countDown()
            }.start()
        }
        countdown.await()
    }

    fun lagDokumentRespons(fom: LocalDate, tom: LocalDate): no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse {
        val dokumentResponse1 = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse1.response = HentDokumentResponse()
        dokumentResponse1.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            listOf(Periode(fom, tom))
        ).toByteArray()
        return dokumentResponse1
    }

    fun lagDokumentRespons(): no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse {
        val dokumentResponse1 = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse1.response = HentDokumentResponse()
        dokumentResponse1.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            ArrayList()
        ).toByteArray()
        return dokumentResponse1
    }

    fun lagDokumentRespons(fom: LocalDate, tom: LocalDate, fom2: LocalDate, tom2: LocalDate): no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse {
        val dokumentResponse1 = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse1.response = HentDokumentResponse()
        dokumentResponse1.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            listOf(Periode(fom, tom), Periode(fom2, tom2))
        ).toByteArray()
        return dokumentResponse1
    }

    private fun inngaaendeJournal(arkivId: String): InngaaendeJournal {
        return InngaaendeJournal(
            dokumentId = arkivId,
            status = JournalStatus.MIDLERTIDIG)
    }

}
