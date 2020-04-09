package no.nav.syfo.utsattoppgave

import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.transaction.Transactional

@KtorExperimentalAPI
@ExtendWith(SpringExtension::class)
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@WebAppConfiguration
@Transactional
open class UtsattOppgaveIntegrasjonsTest  {

    @Autowired
    lateinit var inntektsmeldingRepository: InntektsmeldingRepository
    @Autowired
    lateinit var utsattOppgaveDao: UtsattOppgaveDao

    var journalV2: JournalV2 = mockk() {
        every { hentDokument(any()) } returns HentDokumentResponse()
            .apply {
                dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(
                        Periode(
                            LocalDate.of(2019, 1, 1),
                            LocalDate.of(2019, 1, 16)
                        )
                    )
                ).toByteArray()
            }
    }
    var aktorConsumer: AktorConsumer = mockk {
        every { getAktorId(any()) } answers { "aktorId_for_${arg<String>(0)}" }
    }
    var inngaaendeJournalConsumer: InngaaendeJournalConsumer = mockk() {
        every { hentDokumentId(any()) } answers { inngaaendeJournal(arg(0)) }
    }
    var metrikk: Metrikk = mockk(relaxed = true)
    var inntektsmeldingProducer: InntektsmeldingProducer = mockk(relaxed = true)
    var behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer = mockk(relaxed = true)
    var behandlendeEnhetConsumer: BehandlendeEnhetConsumer = mockk() {
        every { hentBehandlendeEnhet(any()) } returns "enhet"
        every { hentGeografiskTilknytning(any()) } returns GeografiskTilknytningData(
            geografiskTilknytning = "tilknytning",
            diskresjonskode = ""
        )
    }
    var oppgaveClient: OppgaveClient = mockk(relaxed = true)

    var eksisterendeSakService: EksisterendeSakService = mockk(relaxed = true)
    val sakClient: SakClient = mockk()

    lateinit var journalConsumer: JournalConsumer
    lateinit var inntektsmeldingService: InntektsmeldingService
    lateinit var saksbehandlingService: SaksbehandlingService
    lateinit var utsattOppgaveService: UtsattOppgaveService
    lateinit var utsattOppgaveConsumer: UtsattOppgaveConsumer
    lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler
    lateinit var journalpostService: JournalpostService

    @BeforeAll
    fun setup() {
        journalConsumer = JournalConsumer(journalV2, aktorConsumer)
        inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
        journalpostService = JournalpostService(
            inngaaendeJournalConsumer,
            behandleInngaaendeJournalConsumer,
            journalConsumer,
            behandlendeEnhetConsumer,
            metrikk
        )
        saksbehandlingService = SaksbehandlingService(eksisterendeSakService, inntektsmeldingService, sakClient, metrikk)
        utsattOppgaveService = UtsattOppgaveService(utsattOppgaveDao, oppgaveClient, behandlendeEnhetConsumer)
        inntektsmeldingBehandler = InntektsmeldingBehandler(
            journalpostService,
            saksbehandlingService,
            mockk(relaxed = true),
            inntektsmeldingService,
            aktorConsumer,
            inntektsmeldingProducer,
            utsattOppgaveService
        )
        utsattOppgaveConsumer = UtsattOppgaveConsumer(utsattOppgaveService)
    }



    @Test
    fun `utsetter og forkaster oppgave`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_9", "AR-9"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDao.finn(inntektsmeldingId))
        assertEquals(Tilstand.Ny, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(UUID.fromString(inntektsmeldingId), OppdateringstypeDTO.Utsett), mockk(relaxed = true))

        oppgave = utsattOppgaveDao.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Ferdigbehandlet
        ), mockk(relaxed = true)
        )
        oppgave = utsattOppgaveDao.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Forkastet, oppgave.tilstand)
        verifiserOppgaveOpprettet(0)
    }

    @Test
    fun `utsetter og oppretter oppgave`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_10", "AR-10"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDao.finn(inntektsmeldingId))
        assertEquals(Tilstand.Ny, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(UUID.fromString(inntektsmeldingId), OppdateringstypeDTO.Utsett), mockk(relaxed = true))

        oppgave = utsattOppgaveDao.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Opprett
        ), mockk(relaxed = true)
        )
        oppgave = utsattOppgaveDao.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Opprettet, oppgave.tilstand)
        verifiserOppgaveOpprettet(1)
    }

    @Test
    fun `oppretter oppgave direkte`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_10", "AR-10"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDao.finn(inntektsmeldingId))
        assertEquals(Tilstand.Ny, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Opprett
        ), mockk(relaxed = true)
        )
        oppgave = utsattOppgaveDao.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Opprettet, oppgave.tilstand)
        verifiserOppgaveOpprettet(1)
    }

    @Test
    fun `forkaster oppgave direkte`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_10", "AR-10"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDao.finn(inntektsmeldingId))
        assertEquals(Tilstand.Ny, oppgave.tilstand)

        utsattOppgaveConsumer.listen(utsattOppgaveRecord(
            UUID.fromString(inntektsmeldingId),
            OppdateringstypeDTO.Ferdigbehandlet
        ), mockk(relaxed = true)
        )
        oppgave = utsattOppgaveDao.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Forkastet, oppgave.tilstand)
        verifiserOppgaveOpprettet(0)
    }

    private fun verifiserOppgaveOpprettet(antall: Int) {
        verify(exactly = antall) { runBlocking { oppgaveClient.opprettOppgave(any(),any(),any(),any(),any()) } }
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

    companion object {
        @JvmStatic
        fun beforeClass() {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }
}


