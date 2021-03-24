package no.nav.syfo.utsattoppgave
/*
import io.ktor.util.*
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.transaction.annotation.Transactional

@KtorExperimentalAPI
@RunWith(SpringRunner::class)
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@WebAppConfiguration
@Transactional*/
open class UtsattOppgaveIntegrasjonsTestWithMockkNotWorking {

  /*  companion object {
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

    @Autowired
    lateinit var bakgrunnsjobbService: BakgrunnsjobbService

    @Autowired
    lateinit var om: ObjectMapper

    private val mockAcknowledgment = mockk<Acknowledgment>(relaxed = true)

    @KtorExperimentalAPI
    @Before
    fun setup() {
        inntektsmeldingRepository.deleteAll()
        utsattOppgaveDAO.utsattOppgaveRepository.deleteAll()
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

        utsattOppgaveConsumer = UtsattOppgaveConsumer(utsattOppgaveService, bakgrunnsjobbService, om)
        inntektsmeldingBehandler = InntektsmeldingBehandler(
            journalpostService,
            saksbehandlingService,
            metrikk,
            inntektsmeldingService,
            aktorConsumer,
            inntektsmeldingProducer,
            utsattOppgaveService
        )
        MockKAnnotations.init(inntektsmeldingBehandler)
        runBlocking {
            coEvery { sakClient.opprettSak(any(), any()) } returnsMany listOf(
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
        every { inngaaendeJournalConsumer.hentDokumentId("arkivId") } returns inngaaendeJournal("arkivId")
        every { aktorConsumer.getAktorId(any()) } answers { "aktorId_for_" + firstArg() }
        every { eksisterendeSakService.finnEksisterendeSak(any(), any(), any()) } returns null
        every { behandlendeEnhetConsumer.hentBehandlendeEnhet(any(), any()) } returns "enhet"
        every { behandlendeEnhetConsumer.hentGeografiskTilknytning(any()) } returns
            GeografiskTilknytningData(geografiskTilknytning = "tilknytning", diskresjonskode = "")
        val dokumentResponse = dokumentRespons(listOf(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16))))

        every { journalV2.hentDokument(any()) } returns dokumentResponse.response
        every { inngaaendeJournalConsumer.hentDokumentId(any()) } answers { inngaaendeJournal(firstArg()) }
    }

    @Test
    fun `utsetter og forkaster oppgave`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_9", "AR-9"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDAO.finn(inntektsmeldingId))
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Utsett
            ), mockAcknowledgment
        )

        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Ferdigbehandlet
            ), mockAcknowledgment
        )
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

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Utsett
            ), mockAcknowledgment
        )

        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Opprett
            ), mockAcknowledgment
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

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Opprett
            ), mockAcknowledgment
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

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Ferdigbehandlet
            ), mockAcknowledgment
        )
        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Forkastet, oppgave.tilstand)
        verifiserOppgaveOpprettet(0)
    }

    @Test
    fun `Oppretter oppgave for allerede forkastet`() {
        val inntektsmeldingId = requireNotNull(inntektsmeldingBehandler.behandle("arkivId_10", "AR-10"))
        assertNotNull(inntektsmeldingId)

        var oppgave = requireNotNull(utsattOppgaveDAO.finn(inntektsmeldingId))
        assertEquals(Tilstand.Utsatt, oppgave.tilstand)

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Ferdigbehandlet
            ), mockAcknowledgment
        )

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Opprett
            ), mockAcknowledgment
        )
        oppgave = utsattOppgaveDAO.finn(inntektsmeldingId)!!
        assertEquals(Tilstand.Opprettet, oppgave.tilstand)
        verifiserOppgaveOpprettet(1)

        utsattOppgaveConsumer.listen(
            utsattOppgaveRecord(
                UUID.fromString(inntektsmeldingId),
                OppdateringstypeDTO.Opprett
            ), mockAcknowledgment
        )
        verifiserOppgaveOpprettet(1)
    }

    @Test
    fun `null utgåtte oppgaver`() {
        utsattOppgaveService.opprettOppgaverForUtgåtte()
        verifiserOppgaveOpprettet(0)
    }

    @Test
    fun `finner alle utgåtte oppgaver`() {
        listOf(
            enOppgaveEntitet(now().minusHours(1)),
            enOppgaveEntitet(now().minusHours(3)),
            enOppgaveEntitet(now().plusHours(1))
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
            enOppgaveEntitet(now().minusHours(1), inntektsmeldingsId = id1),
            enOppgaveEntitet(now().minusHours(3), inntektsmeldingsId = id2),
            enOppgaveEntitet(now().plusHours(1), inntektsmeldingsId = id3)
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
            enOppgaveEntitet(now().minusHours(1), Tilstand.Opprettet),
            enOppgaveEntitet(now().minusHours(3), Tilstand.Forkastet)
        ).forEach { utsattOppgaveService.opprett(it) }
        utsattOppgaveService.opprettOppgaverForUtgåtte()
        verifiserOppgaveOpprettet(0)
    }

    @Test
    fun `feil i behandling av en oppgave påvirker ikke andre`() {
        val uuid1 = UUID.randomUUID().toString()
        val uuid2 = UUID.randomUUID().toString()
        val uuid3 = UUID.randomUUID().toString()
        val uuid4 = UUID.randomUUID().toString()
        val mockOppgaveResultat = mockk<OppgaveResultat>()
        every {
            runBlocking {
                oppgaveClient.opprettOppgave(
                    sakId = "1",
                    journalpostId = "journalpostId",
                    tildeltEnhetsnr = "enhet",
                    aktoerId = "aktørId",
                    gjelderUtland = false
                )
            }
        } returns mockOppgaveResultat
        every {
            runBlocking {
                oppgaveClient.opprettOppgave(
                    sakId = "2",
                    journalpostId = "journalpostId",
                    tildeltEnhetsnr = "enhet",
                    aktoerId = "aktørId",
                    gjelderUtland = false
                )
            }
        } returns mockOppgaveResultat
        every {
            runBlocking {
                oppgaveClient.opprettOppgave(
                    sakId = "3",
                    journalpostId = "journalpostId",
                    tildeltEnhetsnr = "enhet",
                    aktoerId = "aktørId",
                    gjelderUtland = false
                )
            }
        } throws OpprettOppgaveException("journalpostId", null)
        every {
            runBlocking {
                oppgaveClient.opprettOppgave(
                    sakId = "4",
                    journalpostId = "journalpostId",
                    tildeltEnhetsnr = "enhet",
                    aktoerId = "aktørId",
                    gjelderUtland = false
                )
            }
        } returns mockOppgaveResultat

        listOf(
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                sakId = "1",
                inntektsmeldingsId = uuid1
            ),
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                sakId = "2",
                inntektsmeldingsId = uuid2
            ),
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                sakId = "3",
                inntektsmeldingsId = uuid3
            ),
            enOppgaveEntitet(
                timeout = now().minusHours(3),
                tilstand = Tilstand.Utsatt,
                sakId = "4",
                inntektsmeldingsId = uuid4
            )
        ).forEach { utsattOppgaveService.opprett(it) }
        utsattOppgaveService.opprettOppgaverForUtgåtte()

        assertEquals(Tilstand.Opprettet, utsattOppgaveDAO.finn(uuid1)?.tilstand)
        assertEquals(Tilstand.Opprettet, utsattOppgaveDAO.finn(uuid2)?.tilstand)
        assertEquals(Tilstand.Utsatt, utsattOppgaveDAO.finn(uuid3)?.tilstand)
        assertEquals(Tilstand.Opprettet, utsattOppgaveDAO.finn(uuid4)?.tilstand)
    }

    private fun enOppgaveEntitet(
        timeout: LocalDateTime,
        tilstand: Tilstand = Tilstand.Utsatt,
        inntektsmeldingsId: String = UUID.randomUUID().toString(),
        sakId: String = "saksId"
    ) = UtsattOppgaveEntitet(
        fnr = "fnr",
        sakId = sakId,
        aktørId = "aktørId",
        journalpostId = "journalpostId",
        arkivreferanse = "arkivreferanse",
        timeout = timeout,
        inntektsmeldingId = inntektsmeldingsId,
        tilstand = tilstand
    )

    private fun verifiserOppgaveOpprettet(antall: Int) {
        runBlocking {
            coVerify(exactly = antall) { oppgaveClient.opprettOppgave(any(), any(), any(), any(), any()) }
        }
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
        timeout: LocalDateTime = now()
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
    }*/
}


