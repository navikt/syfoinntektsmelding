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
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.test.LocalApplication
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
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
        `when`(aktorConsumer.getAktorId(anyString())).thenReturn("aktorId_for_fnr")
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
            Mockito.verify<SakClient>(sakClient, Mockito.times(1)).opprettSak("aktorId_for_fnr", "")
        }
    }

//    @Test
//    fun uten_arbeidsgiverperioder() {
//        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
//            Inntektsmelding(
//                id="abc",
//                fnr = "fnr",
//                journalpostId = "arkivId",
//                arsakTilInnsending = "asd",
//                journalStatus = JournalStatus.MIDLERTIDIG,
//                arkivRefereranse = "AR-123",
//                førsteFraværsdag = LocalDate.of(2019,1,1),
//                mottattDato = LocalDateTime.of(2018,12,24, 13,44,55),
//                arbeidsgiverOrgnummer = "987654321"
//            )
//        )
//        `when`(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId") // inntektsmelding.fnr
//        `when`(saksbehandlingService.behandleInntektsmelding(any(), matches("aktorId"), matches("AR-123"))).thenReturn("saksId")
//
//        val dto = inntektsmeldingBehandler.behandle("arkivId", "AR-123")
//        assertNotNull(dto)
//        assertNotNull(dto?.uuid)
//        assertEquals("arkivId", dto?.journalpostId)
//        assertEquals("aktorId", dto?.aktorId)
//        assertEquals("saksId", dto?.sakId)
//
//        assertEquals(0, dto?.arbeidsgiverperioder?.size)
//        assertEquals("987654321", dto?.orgnummer)
//        assertNull(dto?.arbeidsgiverPrivat)
//        assertEquals(LocalDateTime.of(2018,12,24, 13,44,55), dto?.behandlet)
//    }

//    @Test
//    fun flere_arbeidsgiverperiodeer() {
//        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
//            Inntektsmelding(
//                id="abc",
//                fnr = "fnr",
//                journalpostId = "arkivId",
//                arsakTilInnsending = "asd",
//                journalStatus = JournalStatus.MIDLERTIDIG,
//                arkivRefereranse = "AR-123",
//                førsteFraværsdag = LocalDate.of(2019,1,1),
//                mottattDato = LocalDateTime.of(2018,12,24, 13,44,55),
//                arbeidsgiverPrivatFnr = "123456789",
//                arbeidsgiverperioder = listOf(
//                    Periode(LocalDate.of(2019,12,24), LocalDate.of(2019, 12, 28)),
//                    Periode(LocalDate.of(2018,12,24), LocalDate.of(2018, 12, 28))
//                )
//            )
//        )
//        `when`(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId") // inntektsmelding.fnr
//        `when`(saksbehandlingService.behandleInntektsmelding(any(), matches("aktorId"), matches("AR-123"))).thenReturn("saksId")
//
//        val dto = inntektsmeldingBehandler.behandle("arkivId", "AR-123")
//        assertNotNull(dto)
//        assertNotNull(dto?.uuid)
//        assertEquals("arkivId", dto?.journalpostId)
//        assertEquals("aktorId", dto?.aktorId)
//        assertEquals("saksId", dto?.sakId)
//
//        assertEquals(2, dto?.arbeidsgiverperioder?.size)
//        assertNull(dto?.orgnummer)
//        assertEquals("123456789", dto?.arbeidsgiverPrivat)
//        assertEquals(LocalDateTime.of(2018,12,24, 13,44,55), dto?.behandlet)
//    }
//
//    @Test
//    fun privat_arbeidsgiver() {
//        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
//            Inntektsmelding(
//                id="abc",
//                fnr = "fnr",
//                journalpostId = "arkivId",
//                arsakTilInnsending = "asd",
//                journalStatus = JournalStatus.MIDLERTIDIG,
//                arkivRefereranse = "AR-123",
//                førsteFraværsdag = LocalDate.of(2019,1,1),
//                mottattDato = LocalDateTime.of(2018,12,24, 13,44,55),
//                arbeidsgiverPrivatFnr = "123456789"
//            )
//        )
//        `when`(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId") // inntektsmelding.fnr
//        `when`(saksbehandlingService.behandleInntektsmelding(any(), matches("aktorId"), matches("AR-123"))).thenReturn("saksId")
//
//        val dto = inntektsmeldingBehandler.behandle("arkivId", "AR-123")
//        assertNotNull(dto)
//        assertNotNull(dto?.uuid)
//        assertEquals("arkivId", dto?.journalpostId)
//        assertEquals("aktorId", dto?.aktorId)
//        assertEquals("saksId", dto?.sakId)
//
//        assertEquals(0, dto?.arbeidsgiverperioder?.size)
//        assertNull(dto?.orgnummer)
//        assertEquals("123456789", dto?.arbeidsgiverPrivat)
//        assertEquals(LocalDateTime.of(2018,12,24, 13,44,55), dto?.behandlet)
//    }
//
//
//    @Test
//    fun gjenbruker_SaksIdHvisViFar_ToOverlappendeInntektsmeldinger() {
//        `when`(journalpostService.hentInntektsmelding("arkivId1")).thenReturn(
//            Inntektsmelding(
//                id="abc",
//                aktorId = "aktorId_for_fnr",
//                fnr = "fnr",
//                journalpostId = "arkivId1",
//                arsakTilInnsending = "asd",
//                journalStatus = JournalStatus.MIDLERTIDIG,
//                arkivRefereranse = "AR-123",
//                førsteFraværsdag = LocalDate.of(2019,1,1),
//                mottattDato = LocalDateTime.of(2018,12,24, 13,44,55),
//                arbeidsgiverPrivatFnr = "123456789",
//                arbeidsgiverperioder = listOf(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16)))
//            )
//        )
//        `when`(journalpostService.hentInntektsmelding("arkivId2")).thenReturn(
//            Inntektsmelding(
//                id="abc",
//                aktorId = "aktorId_for_fnr",
//                fnr = "fnr",
//                journalpostId = "arkivId2",
//                arsakTilInnsending = "asd",
//                journalStatus = JournalStatus.MIDLERTIDIG,
//                arkivRefereranse = "AR-123",
//                førsteFraværsdag = LocalDate.of(2019,1,1),
//                mottattDato = LocalDateTime.of(2018,12,24, 13,44,55),
//                arbeidsgiverPrivatFnr = "123456789",
//                arbeidsgiverperioder = listOf(Periode(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 16)))
//            )
//        )
//
//
//        `when`(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId_for_fnr") // inntektsmelding.fnr
//        `when`(saksbehandlingService.behandleInntektsmelding(any(), matches("aktorId_for_fnr"), matches("AR-123"))).thenReturn("saksId")
//
//        val inntektsmeldingMetas = inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId_for_fnr")
//        Assertions.assertThat(inntektsmeldingMetas.size).isEqualTo(2)
//        Assertions.assertThat(inntektsmeldingMetas[0].sakId).isEqualTo(inntektsmeldingMetas[1].sakId)
//    }
//
//    @Test
//    fun gjenbruker_Ikke_SaksIdHvisViFarToInntektsmeldingerSomIkkeOverlapper() {
//    }
//
//    @Test
//    fun brukerSaksIdFraSykeforloepOmViIkkeHarOverlappendeInntektsmelding() {
//    }
//
//
//    @Test
//    fun somEnSakMedVedLikPeriode() {
//    }
//
//    @Test
//    fun forFlerPersonerSamtidig() {
//    }

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
