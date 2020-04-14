package no.nav.syfo.behandling

import any
import eq
import kotlinx.coroutines.runBlocking
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
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentRequest
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger


@RunWith(SpringRunner::class)
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@WebAppConfiguration
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

    @MockBean
    lateinit var journalV2: JournalV2
    @MockBean
    lateinit var aktorConsumer: AktorConsumer
    @MockBean
    lateinit var inngaaendeJournalConsumer: InngaaendeJournalConsumer
    lateinit var journalConsumer: JournalConsumer

    lateinit var saksbehandlingService: SaksbehandlingService

    @MockBean
    lateinit var metrikk: Metrikk

    @MockBean
    lateinit var inntektsmeldingProducer: InntektsmeldingProducer
    @MockBean
    lateinit var behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer
    @MockBean
    lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer
    @MockBean
    lateinit var oppgaveClient: OppgaveClient

    @MockBean
    lateinit var sakClient : SakClient
    @MockBean
    lateinit var eksisterendeSakService: EksisterendeSakService

    @Autowired
    lateinit var inntektsmeldingRepository: InntektsmeldingRepository
    lateinit var inntektsmeldingService: InntektsmeldingService

    @MockBean
    lateinit var journalpostService: JournalpostService

    lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @Before
    fun setup() {
        inntektsmeldingRepository.deleteAll()
        journalConsumer = JournalConsumer(journalV2, aktorConsumer)
        journalpostService = JournalpostService(inngaaendeJournalConsumer, behandleInngaaendeJournalConsumer, journalConsumer, behandlendeEnhetConsumer, metrikk)
        inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository, 3)
        saksbehandlingService = SaksbehandlingService(oppgaveClient, behandlendeEnhetConsumer, eksisterendeSakService, inntektsmeldingService, sakClient, metrikk)
        inntektsmeldingBehandler = InntektsmeldingBehandler(journalpostService, saksbehandlingService, metrikk, inntektsmeldingService, aktorConsumer, inntektsmeldingProducer)
        MockitoAnnotations.initMocks(inntektsmeldingBehandler)
        runBlocking {
            given(sakClient.opprettSak(anyString(), anyString())).willReturn(
                SakResponse(id = 987, tema = "SYM", aktoerId = "444", applikasjon = "", fagsakNr = "123000", opprettetAv = "meg", opprettetTidspunkt = ZonedDateTime.now(), orgnr = "999888777"),
                SakResponse(id = 988, tema = "SYM", aktoerId = "444", applikasjon = "", fagsakNr = "123000", opprettetAv = "meg", opprettetTidspunkt = ZonedDateTime.now(), orgnr = "999888777")
            )
        }
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId")).thenReturn(inngaaendeJournal("arkivId"))
        given(aktorConsumer.getAktorId(anyString())).willAnswer { "aktorId_for_" + it.getArgument(0) }
        given(eksisterendeSakService.finnEksisterendeSak(anyString(), any(), any())).willReturn(null)
        `when`(behandlendeEnhetConsumer.hentBehandlendeEnhet(anyString())).thenReturn("enhet")
        `when`(behandlendeEnhetConsumer.hentGeografiskTilknytning(anyString())).thenReturn(
            GeografiskTilknytningData(geografiskTilknytning = "tilknytning", diskresjonskode = "")
        )
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Gjenbruker saksId hvis vi får to overlappende inntektsmeldinger`() {
        given(aktorConsumer.getAktorId(anyString())).willReturn("aktorId_for_1", "aktorId_for_1")
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"))
        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 16))
        `when`(journalV2.hentDokument(any())).thenReturn(
            dokumentResponse1.response,
            dokumentResponse2.response
        )
        inntektsmeldingBehandler.behandle("arkivId1", "AR-1")
        inntektsmeldingBehandler.behandle("arkivId2", "AR-2")
        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_1")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo(inntektsmeldingMetas[1].sakId)
        runBlocking {
            verify<SakClient>(sakClient, Mockito.times(1)).opprettSak(eq("aktorId_for_1"), any())
        }
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Gjenbruker ikke saksId hvis vi får to inntektsmeldinger som ikke overlapper`() {
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId3")).thenReturn(inngaaendeJournal("arkivId3"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId4")).thenReturn(inngaaendeJournal("arkivId4"))

        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16))

        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse1.response,
            dokumentResponse2.response
        )

        given(aktorConsumer.getAktorId(ArgumentMatchers.anyString())).willReturn("778", "778")

        inntektsmeldingBehandler.behandle("arkivId3", "AR-3")
        inntektsmeldingBehandler.behandle("arkivId4", "AR-4")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("778")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        Assertions.assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("988")

        runBlocking {
            verify<SakClient>(sakClient, Mockito.times(2)).opprettSak(eq("778"), anyString())
        }
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Bruker saksId fra sykeforløp om vi ikke har overlappende inntektsmelding`() {
        given(eksisterendeSakService.finnEksisterendeSak(any(), Mockito.any(), Mockito.any())).willReturn(null, "syfosak")

        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId5")).thenReturn(inngaaendeJournal("arkivId5"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId6")).thenReturn(inngaaendeJournal("arkivId6"))

        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16))

        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse1.response,
            dokumentResponse2.response
        )
        given(aktorConsumer.getAktorId(anyString())).willReturn("999", "999")

        inntektsmeldingBehandler.behandle("arkivId5", "AR-5")
        inntektsmeldingBehandler.behandle("arkivId6", "AR-6")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("999")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        Assertions.assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("syfosak")

        runBlocking {
            verify<SakClient>(sakClient, Mockito.times(1)).opprettSak(anyString(), anyString())
        }
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun `Mottar inntektsmelding uten arbeidsgiverperioder`() {
        given(aktorConsumer.getAktorId(anyString())).willReturn("aktorId_for_7", "aktorId_for_7")
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId7")).thenReturn(inngaaendeJournal("arkivId7"))
        val dokumentResponse = lagDokumentRespons()
        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse.response
        )
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
        given(aktorConsumer.getAktorId(anyString())).willReturn("aktorId_for_8", "aktorId_for_8")
        val dokumentResponse = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 14))
        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse.response
        )

        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId_8")).thenReturn(inngaaendeJournal("arkivId_8"))

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
        given(aktorConsumer.getAktorId(anyString())).willReturn("aktorId_for_9", "aktorId_for_9")
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse.response = HentDokumentResponse()
        dokumentResponse.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiverPrivat().toByteArray()
        `when`(journalV2.hentDokument(any())).thenReturn(
            dokumentResponse.response
        )
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId_9")).thenReturn(inngaaendeJournal("arkivId_9"))

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
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId")).thenReturn(inngaaendeJournal("arkivId"))
        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse.response
        )

        val numThreads = 16
        produceParallelMessages(numThreads, "arkivId")

        runBlocking {
            verify<SakClient>(sakClient, Mockito.times(1)).opprettSak(anyString(), anyString())
        }
        verify<BehandleInngaaendeJournalConsumer>(behandleInngaaendeJournalConsumer, Mockito.times(numThreads)).ferdigstillJournalpost(
            any()
        )
    }

    @Test
    @Throws(Exception::class)
    fun `Behandler inntektsmeldinger for flere personer samtidig`() = runBlocking {
        val fnr = AtomicInteger()
        given(journalV2.hentDokument(Mockito.any<HentDokumentRequest>())).willAnswer {
            build(fnr)
        }

        val numThreads = 16
        produceParallelMessages(numThreads, "arkivId")
        verify<SakClient>(sakClient, Mockito.times(numThreads)).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        verify<BehandleInngaaendeJournalConsumer>(behandleInngaaendeJournalConsumer, Mockito.times(numThreads)).ferdigstillJournalpost(
            any()
        )
    }

    fun build(fnr: AtomicInteger): no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse {
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse()
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
