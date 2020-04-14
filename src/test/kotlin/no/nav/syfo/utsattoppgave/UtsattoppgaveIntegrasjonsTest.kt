package no.nav.syfo.utsattoppgave

import io.ktor.util.KtorExperimentalAPI
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.SakResponse
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumerTest
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.support.Acknowledgment
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

@KtorExperimentalAPI
@RunWith(SpringRunner::class)
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@WebAppConfiguration
@Transactional
open class UtsattOppgaveIntegrasjonsTest  {

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
    lateinit var sakClient: SakClient

    @MockBean
    lateinit var eksisterendeSakService: EksisterendeSakService

    @MockBean
    lateinit var journalpostService: JournalpostService

    lateinit var journalConsumer: JournalConsumer
    lateinit var saksbehandlingService: SaksbehandlingService

    @Autowired
    lateinit var utsattOppgaveDAO: UtsattOppgaveDAO
    @Autowired
    lateinit var utsattOppgaveService: UtsattOppgaveService
    lateinit var utsattOppgaveConsumer: UtsattOppgaveConsumer

    @Autowired
    lateinit var inntektsmeldingRepository: InntektsmeldingRepository
    lateinit var inntektsmeldingService: InntektsmeldingService
    lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @KtorExperimentalAPI
    @Before
    fun setup() {
        inntektsmeldingRepository.deleteAll()
        journalConsumer = JournalConsumer(journalV2, aktorConsumer)
        journalpostService = JournalpostService(
            inngaaendeJournalConsumer,
            behandleInngaaendeJournalConsumer,
            journalConsumer,
            behandlendeEnhetConsumer,
            metrikk
        )
        inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository, 3)
        saksbehandlingService =
            SaksbehandlingService(eksisterendeSakService, inntektsmeldingService, sakClient, metrikk)
        utsattOppgaveConsumer = UtsattOppgaveConsumer(utsattOppgaveService)
        inntektsmeldingBehandler = InntektsmeldingBehandler(
            journalpostService,
            saksbehandlingService,
            metrikk,
            inntektsmeldingService,
            aktorConsumer,
            inntektsmeldingProducer,
            utsattOppgaveService
        )
        MockitoAnnotations.initMocks(inntektsmeldingBehandler)
        runBlocking {
            BDDMockito.given(sakClient.opprettSak(BDDMockito.anyString(), BDDMockito.anyString())).willReturn(
                SakResponse(
                    id = 987,
                    tema = "SYM",
                    aktoerId = "444",
                    applikasjon = "",
                    fagsakNr = "123000",
                    opprettetAv = "meg",
                    opprettetTidspunkt = ZonedDateTime.now(),
                    orgnr = "999888777"
                ),
                SakResponse(
                    id = 988,
                    tema = "SYM",
                    aktoerId = "444",
                    applikasjon = "",
                    fagsakNr = "123000",
                    opprettetAv = "meg",
                    opprettetTidspunkt = ZonedDateTime.now(),
                    orgnr = "999888777"
                )
            )
        }
        Mockito.`when`(inngaaendeJournalConsumer.hentDokumentId("arkivId")).thenReturn(inngaaendeJournal("arkivId"))
        BDDMockito.given(aktorConsumer.getAktorId(BDDMockito.anyString())).willAnswer { "aktorId_for_" + it.getArgument(0) }
        BDDMockito.given(eksisterendeSakService.finnEksisterendeSak(BDDMockito.anyString(), any(), any())).willReturn(null)
        Mockito.`when`(behandlendeEnhetConsumer.hentBehandlendeEnhet(BDDMockito.anyString())).thenReturn("enhet")
        Mockito.`when`(behandlendeEnhetConsumer.hentGeografiskTilknytning(BDDMockito.anyString())).thenReturn(
            GeografiskTilknytningData(geografiskTilknytning = "tilknytning", diskresjonskode = "")
        )
        val dokumentResponse = dokumentRespons(listOf(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))))

        Mockito.`when`(journalV2.hentDokument(Mockito.any())).thenReturn(dokumentResponse.response)
        BDDMockito.given(inngaaendeJournalConsumer.hentDokumentId(BDDMockito.anyString())).willAnswer { inngaaendeJournal(it.getArgument(0)) }
    }

    @Test
    fun `utsetter og forkaster oppgave`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_9", "AR-9"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDAO.finn(inntektsmeldingId))
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(UUID.fromString(inntektsmeldingId), OppdateringstypeDTO.Utsett), mock(Acknowledgment::class.java))

        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Ferdigbehandlet
        ), mock(Acknowledgment::class.java))
        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Forkastet, oppgave.tilstand)
        verifiserOppgaveOpprettet(0)
    }

    @Test
    fun `utsetter og oppretter oppgave`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_10", "AR-10"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDAO.finn(inntektsmeldingId))
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(UUID.fromString(inntektsmeldingId), OppdateringstypeDTO.Utsett), mock(Acknowledgment::class.java))

        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Opprett
        ), mock(Acknowledgment::class.java)
        )
        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Opprettet, oppgave.tilstand)
        verifiserOppgaveOpprettet(1)
    }

    @Test
    fun `oppretter oppgave direkte`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_10", "AR-10"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDAO.finn(inntektsmeldingId))
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Opprett
        ), mock(Acknowledgment::class.java)
        )
        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Opprettet, oppgave.tilstand)
        verifiserOppgaveOpprettet(1)
    }

    @Test
    fun `forkaster oppgave direkte`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_10", "AR-10"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDAO.finn(inntektsmeldingId))
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Ferdigbehandlet
        ), mock(Acknowledgment::class.java)
        )
        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Forkastet, oppgave.tilstand)
        verifiserOppgaveOpprettet(0)
    }

    @Test
    fun `null utgåtte oppgaver`() {
        utsattOppgaveService.opprettOppgaverForUtgåtte()
        verifiserOppgaveOpprettet(0)
    }

    @Test
    fun `finner alle utgåtte oppgaver`() {
        listOf(
            enOppgaveEntitet(LocalDateTime.now().minusHours(1)),
            enOppgaveEntitet(LocalDateTime.now().minusHours(3)),
            enOppgaveEntitet(LocalDateTime.now().plusHours(1))
        ).forEach { utsattOppgaveService.opprett(it) }
        utsattOppgaveService.opprettOppgaverForUtgåtte()
        verifiserOppgaveOpprettet(2)
    }

    @Test
    fun `oppretter ikke oppgaver dobbelt opp`() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val id3 = UUID.randomUUID().toString()
        listOf(
            enOppgaveEntitet(LocalDateTime.now().minusHours(1), inntektsmeldingsId = id1),
            enOppgaveEntitet(LocalDateTime.now().minusHours(3), inntektsmeldingsId = id2),
            enOppgaveEntitet(LocalDateTime.now().plusHours(1), inntektsmeldingsId = id3)
        ).forEach { utsattOppgaveService.opprett(it) }
        utsattOppgaveService.opprettOppgaverForUtgåtte()
        val oppgave1 = utsattOppgaveDAO.finn(id1)
        val oppgave2 = utsattOppgaveDAO.finn(id2)
        val oppgave3 = utsattOppgaveDAO.finn(id3)
        assertEquals(Tilstand.Opprettet, oppgave1?.tilstand)
        assertEquals(Tilstand.Opprettet, oppgave2?.tilstand)
        assertEquals(Tilstand.Utsatt, oppgave3?.tilstand)

    }

    @Test
    fun `utgåtte oppgaver med feil tilstand opprettes ikke`() {
        listOf(
            enOppgaveEntitet(LocalDateTime.now().minusHours(1), Tilstand.Opprettet),
            enOppgaveEntitet(LocalDateTime.now().minusHours(3), Tilstand.Forkastet)
        ).forEach { utsattOppgaveService.opprett(it) }
        utsattOppgaveService.opprettOppgaverForUtgåtte()
        verifiserOppgaveOpprettet(0)
    }

    private fun enOppgaveEntitet(
        timeout: LocalDateTime,
        tilstand: Tilstand = Tilstand.Utsatt,
        inntektsmeldingsId: String = UUID.randomUUID().toString()
    ) = UtsattOppgaveEntitet(
        fnr = "fnr",
        sakId = "saksId",
        aktørId = "aktørId",
        journalpostId = "journalpostId",
        arkivreferanse = "arkivreferanse",
        timeout = timeout,
        inntektsmeldingId = inntektsmeldingsId,
        tilstand = tilstand
    )

    private fun verifiserOppgaveOpprettet(antall: Int) {
        runBlocking { Mockito.verify(oppgaveClient, times(antall)).opprettOppgave(anyString(), anyString(), anyString(), anyString(), anyBoolean()) }
    }

    private fun utsattOppgaveRecord(id: UUID, oppdateringstype: OppdateringstypeDTO) = ConsumerRecord(
        "topic",
        0,
        0,
        "key",
        utsattOppgave(dokumentType = DokumentTypeDTO.Inntektsmelding, id = id, oppdateringstype = oppdateringstype)
    )

    private fun utsattOppgave(
        dokumentType: DokumentTypeDTO = DokumentTypeDTO.Inntektsmelding,
        oppdateringstype: OppdateringstypeDTO = OppdateringstypeDTO.Utsett,
        id: UUID = UUID.randomUUID(),
        timeout: LocalDateTime = LocalDateTime.now()
    ) = UtsattOppgaveDTO(
        dokumentType = dokumentType,
        oppdateringstype = oppdateringstype,
        dokumentId = id,
        timeout = timeout
    )

    private fun inngaaendeJournal(arkivId: String): InngaaendeJournal {
        return InngaaendeJournal(
            dokumentId = arkivId,
            status = JournalStatus.MIDLERTIDIG
        )
    }

    private fun dokumentRespons(perioder: List<Periode>): no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse {
        val respons = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        respons.response = HentDokumentResponse()
        respons.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(perioder).toByteArray()
        return respons
    }
}


