package no.nav.syfo.behandling

import any
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.SakResponse
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumerTest
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer
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
import no.nav.syfo.test.LocalApplication
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentRequest
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import javax.jms.MessageNotWriteableException


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@EnableJpaRepositories("no.nav.syfo")
@EntityScan(basePackages = ["no.nav.syfo.dto"])
@ActiveProfiles("test")
open class InntektsmeldingBehandlerIntegrasjonsTest {

    @MockBean
    lateinit var journalV2: JournalV2
    @MockBean
    lateinit var aktorConsumer: AktorConsumer
    @MockBean
    lateinit var inngaaendeJournalConsumer: InngaaendeJournalConsumer
    lateinit var journalConsumer: JournalConsumer

//    @Autowired
    lateinit var saksbehandlingService: SaksbehandlingService

    @MockBean
    lateinit var metrikk: Metrikk

    @MockBean
    private lateinit var behandleInngaaendeJournalV1: BehandleInngaaendeJournalV1

    @MockBean
    lateinit var inntektsmeldingProducer: InntektsmeldingProducer
    @MockBean
    lateinit var behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer
    @MockBean
    lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer
    @MockBean
    lateinit var oppgavebehandlingConsumer: OppgavebehandlingConsumer

    @MockBean
    lateinit var sakClient : SakClient
    @MockBean
    lateinit var eksisterendeSakService: EksisterendeSakService
    @MockBean
    lateinit var inngaaendeJournalV1: InngaaendeJournalV1

    @Autowired
    lateinit var inntektsmeldingRepository: InntektsmeldingRepository
    lateinit var inntektsmeldingService: InntektsmeldingService


    @MockBean
    lateinit var journalpostService: JournalpostService

    lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @Before
    fun init() {
    }

    @Before
    fun setup() {
        journalConsumer = JournalConsumer(journalV2, aktorConsumer)
        journalpostService = JournalpostService(inngaaendeJournalConsumer, behandleInngaaendeJournalConsumer, journalConsumer, behandlendeEnhetConsumer, metrikk)
        inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
        saksbehandlingService = SaksbehandlingService(oppgavebehandlingConsumer, behandlendeEnhetConsumer, eksisterendeSakService, inntektsmeldingService, sakClient, metrikk)
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
    fun gjenbrukerSaksIdHvisViFarToOverlappendeInntektsmeldinger() {
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"))
        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 16))
        `when`(journalV2.hentDokument(any())).thenReturn(
            dokumentResponse1.response,
            dokumentResponse2.response
        )
        inntektsmeldingBehandler.behandle("arkivId1", "AR-123")
        inntektsmeldingBehandler.behandle("arkivId2", "AR-124")
        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_fnr")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo(inntektsmeldingMetas[1].sakId)
        runBlocking {
            Mockito.verify<SakClient>(sakClient, Mockito.times(1)).opprettSak("aktorId_for_fnr", any())
        }
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun gjenbrukerIkkeSaksIdHvisViFarToInntektsmeldingerSomIkkeOverlapper() {
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId3")).thenReturn(inngaaendeJournal("arkivId3"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId4")).thenReturn(inngaaendeJournal("arkivId4"))

        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16))

        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse1.response,
            dokumentResponse2.response
        )

        given(aktorConsumer.getAktorId(ArgumentMatchers.anyString())).willReturn("778", "778")

        inntektsmeldingBehandler.behandle("arkivId3", "AR-123")
        inntektsmeldingBehandler.behandle("arkivId4", "AR-123")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("778")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        Assertions.assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("988")

        runBlocking {
            Mockito.verify<SakClient>(sakClient, Mockito.times(2)).opprettSak("778", "AR-123")
        }
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun brukerSaksIdFraSykeforloepOmViIkkeHarOverlappendeInntektsmelding() {
        given(eksisterendeSakService.finnEksisterendeSak(Mockito.any(), Mockito.any(), Mockito.any())).willReturn(null, "syfosak")

        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"))

        val dokumentResponse1 = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse1.response = HentDokumentResponse()
        dokumentResponse1.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            listOf(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16)))
        ).toByteArray()

        val dokumentResponse2 = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse2.response = HentDokumentResponse()
        dokumentResponse2.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            listOf(Periode(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16)))
        ).toByteArray()

        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse1.response,
            dokumentResponse2.response
        )
        given(aktorConsumer.getAktorId(ArgumentMatchers.anyString())).willReturn("999", "999")

        inntektsmeldingBehandler.behandle("arkivId1", "AR-123")
        inntektsmeldingBehandler.behandle("arkivId2", "AR-124")

        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("999")
        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        Assertions.assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("syfosak")

        runBlocking {
            Mockito.verify<SakClient>(sakClient, Mockito.times(1)).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun mottarInntektsmeldingUtenArbeidsgiverperioder() {
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse.response = HentDokumentResponse()
        dokumentResponse.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            Collections.emptyList<Periode>()
        ).toByteArray()
        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse.response
        )

        inntektsmeldingBehandler.behandle("arkivId1", "AR-123")

        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    @Throws(
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun mottarInntektsmeldingMedFlerePerioder() {
        val dokumentResponse1 = lagDokumentRespons(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 12))
        val dokumentResponse2 = lagDokumentRespons(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 14))
        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse1.response,
            dokumentResponse2.response
        )

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(2)
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019, 1, 1))
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019, 1, 12))
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[1].fom).isEqualTo(LocalDate.of(2019, 1, 12))
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverperioder[1].tom).isEqualTo(LocalDate.of(2019, 1, 14))
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun mottarInntektsmeldingMedPrivatArbeidsgiver() {
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse.response = HentDokumentResponse()
        dokumentResponse.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiverPrivat().toByteArray()

        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse.response
        )

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        val inntektsmeldinger = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverPrivatFnr).isEqualTo("arbeidsgiverPrivat")
        Assertions.assertThat(inntektsmeldinger[0].arbeidsgiverOrgnummer).isNull()
        Assertions.assertThat(inntektsmeldinger[0].aktorId).isEqualTo("aktorId_for_fnr")
    }

    @Test
    @Throws(Exception::class)
    fun behandlerInntektsmeldingSomEnSakMedVedLikPeriode() {
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse.response = HentDokumentResponse()
        dokumentResponse.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
            listOf(
                Periode(LocalDate.now(), LocalDate.now().plusDays(20))
            )
        ).toByteArray()

        `when`(journalV2.hentDokument(Mockito.any())).thenReturn(
            dokumentResponse.response
        )

        val numThreads = 16
        produceParallelMessages(numThreads)

        runBlocking {
            Mockito.verify<SakClient>(sakClient, Mockito.times(1)).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
        Mockito.verify<BehandleInngaaendeJournalV1>(behandleInngaaendeJournalV1, Mockito.times(numThreads)).ferdigstillJournalfoering(
            Mockito.any()
        )
    }

    @Test
    @Throws(Exception::class)
    fun behandlerInntektsmeldingerForFlerPersonerSamtidig() {
        val fnr = AtomicInteger()
        given(journalV2.hentDokument(Mockito.any<HentDokumentRequest>())).willAnswer {
            build(fnr)
        }

        val numThreads = 16
        produceParallelMessages(numThreads)
        runBlocking {
            Mockito.verify<SakClient>(sakClient, Mockito.times(numThreads)).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
        Mockito.verify<BehandleInngaaendeJournalV1>(behandleInngaaendeJournalV1, Mockito.times(numThreads)).ferdigstillJournalfoering(
            Mockito.any()
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
    fun produceParallelMessages(numThreads: Int) {
        val countdown = CountDownLatch(numThreads)

        repeat(numThreads) {
            Thread {
                try {
                    inntektsmeldingBehandler.behandle("arkivId", "AR-123r")
                } catch (e: MessageNotWriteableException) {
                    e.printStackTrace()
                }

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

    private fun inngaaendeJournal(arkivId: String): InngaaendeJournal {
        return InngaaendeJournal(
            dokumentId = arkivId,
            status = JournalStatus.MIDLERTIDIG)
    }

}
