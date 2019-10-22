package no.nav.syfo.consumer.mq

import any
import kotlinx.coroutines.runBlocking
import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.SakResponse
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumerTest
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingDAO
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentRequest
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse
import org.apache.activemq.command.ActiveMQTextMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Arrays.asList
import java.util.Collections.emptyList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.jms.MessageNotWriteableException

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
class InntektsmeldingConsumerIntegrassjonsTest {

    @MockBean
    private lateinit var inngaaendeJournalConsumer: InngaaendeJournalConsumer

    @MockBean
    private lateinit var journalV2: JournalV2

    @MockBean
    private lateinit var aktorConsumer: AktorConsumer

    @MockBean
    private lateinit var eksisterendeSakService: EksisterendeSakService

    @MockBean
    private lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer

    @MockBean
    private lateinit var oppgavebehandlingConsumer: OppgavebehandlingConsumer

    @MockBean
    private lateinit var behandleInngaaendeJournalV1: BehandleInngaaendeJournalV1

    @MockBean
    private val metrikk: Metrikk? = null

    @Inject
    private lateinit var inntektsmeldingConsumer: InntektsmeldingConsumer

    @MockBean
    private lateinit var inntektsmeldingProducer: InntektsmeldingProducer

    @Inject
    private lateinit var inntektsmeldingDAO: InntektsmeldingDAO

    @Inject
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockBean
    private lateinit var oppgaveClient : OppgaveClient

    @MockBean
    private lateinit var sakClient : SakClient

    @Before
    fun setup() {
        jdbcTemplate.update("DELETE FROM ARBEIDSGIVERPERIODE")
        jdbcTemplate.update("DELETE FROM INNTEKTSMELDING")

        val inngaaendeJournal = inngaaendeJournal("arkivId")

        runBlocking {
            given(sakClient.opprettSak( any(), any())).willReturn(
                    SakResponse(id = 987, tema = "SYM", aktoerId = "444", applikasjon = "", fagsakNr = "123000", opprettetAv = "meg", opprettetTidspunkt = ZonedDateTime.now(), orgnr = "999888777"),
                    SakResponse(id = 988, tema = "SYM", aktoerId = "444", applikasjon = "", fagsakNr = "123000", opprettetAv = "meg", opprettetTidspunkt = ZonedDateTime.now(), orgnr = "999888777")
            )
        }

        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId")).thenReturn(inngaaendeJournal)

        given(aktorConsumer.getAktorId(ArgumentMatchers.anyString())).willAnswer { "aktorId_for_" + it.getArgument(0) }

        given(eksisterendeSakService.finnEksisterendeSak(ArgumentMatchers.anyString(), any(), any())).willReturn(null)

        `when`(behandlendeEnhetConsumer.hentBehandlendeEnhet(ArgumentMatchers.anyString())).thenReturn("enhet")
        `when`(behandlendeEnhetConsumer.hentGeografiskTilknytning(ArgumentMatchers.anyString())).thenReturn(
                GeografiskTilknytningData(geografiskTilknytning = "tilknytning", diskresjonskode = "")
        )
    }

    private fun inngaaendeJournal(arkivId: String): InngaaendeJournal {
        return InngaaendeJournal(
                dokumentId = arkivId,
                status = JournalStatus.MIDLERTIDIG)
    }

    @Test
    @Throws(
            MessageNotWriteableException::class,
            HentDokumentSikkerhetsbegrensning::class,
            HentDokumentDokumentIkkeFunnet::class
    )
    fun gjenbrukerSaksIdHvisViFarToOverlappendeInntektsmeldinger() {
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
                listOf(Periode(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 16)))
        ).toByteArray()

        `when`(journalV2.hentDokument(any())).thenReturn(
                dokumentResponse1.response,
                dokumentResponse2.response
        )

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"))
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"))

        val inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId_for_fnr")
        assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        assertThat(inntektsmeldingMetas[0].sakId).isEqualTo(inntektsmeldingMetas[1].sakId)

        runBlocking {
            verify<SakClient>(sakClient, times(1)).opprettSak("aktorId_for_fnr", "")
        }
    }

    @Test
    @Throws(
            MessageNotWriteableException::class,
            HentDokumentSikkerhetsbegrensning::class,
            HentDokumentDokumentIkkeFunnet::class
    )
    fun gjenbrukerIkkeSaksIdHvisViFarToInntektsmeldingerSomIkkeOverlapper() {
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

        `when`(journalV2.hentDokument(any())).thenReturn(
                dokumentResponse1.response,
                dokumentResponse2.response
        )

        given(aktorConsumer.getAktorId(ArgumentMatchers.anyString())).willReturn("777", "777")

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"))
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"))

        val inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("777")
        assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("988")

        runBlocking {
            verify<SakClient>(sakClient, times(2)).opprettSak("777", "")
        }
    }

    @Test
    @Throws(
            MessageNotWriteableException::class,
            HentDokumentSikkerhetsbegrensning::class,
            HentDokumentDokumentIkkeFunnet::class
    )
    fun brukerSaksIdFraSykeforloepOmViIkkeHarOverlappendeInntektsmelding() {
        given(eksisterendeSakService.finnEksisterendeSak(any(), any(), any())).willReturn(null, "syfosak")

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

        `when`(journalV2.hentDokument(any())).thenReturn(
                dokumentResponse1.response,
                dokumentResponse2.response
        )
        given(aktorConsumer.getAktorId(ArgumentMatchers.anyString())).willReturn("999", "999")

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"))
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"))

        val inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("999")
        assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("987")
        assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("syfosak")

        runBlocking {
            verify<SakClient>(sakClient, times(1)).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
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
                emptyList<Periode>()
        ).toByteArray()
        `when`(journalV2.hentDokument(any())).thenReturn(
                dokumentResponse.response
        )

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"))

        val inntektsmeldinger = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

        assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    @Throws(
            MessageNotWriteableException::class,
            HentDokumentSikkerhetsbegrensning::class,
            HentDokumentDokumentIkkeFunnet::class
    )
    fun mottarInntektsmeldingMedFlerePerioder() {
        val dokumentResponse = no.nav.tjeneste.virksomhet.journal.v2.HentDokumentResponse()
        dokumentResponse.response = HentDokumentResponse()
        dokumentResponse.response.dokument = JournalConsumerTest.inntektsmeldingArbeidsgiver(
                asList(
                        Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 12)),
                        Periode(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 14))
                )
        ).toByteArray()

        `when`(journalV2.hentDokument(any())).thenReturn(dokumentResponse.response)

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"))

        val inntektsmeldinger = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

        assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(2)
        assertThat(inntektsmeldinger[0].arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019, 1, 1))
        assertThat(inntektsmeldinger[0].arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019, 1, 12))
        assertThat(inntektsmeldinger[0].arbeidsgiverperioder[1].fom).isEqualTo(LocalDate.of(2019, 1, 12))
        assertThat(inntektsmeldinger[0].arbeidsgiverperioder[1].tom).isEqualTo(LocalDate.of(2019, 1, 14))
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

        `when`(journalV2.hentDokument(any())).thenReturn(
                dokumentResponse.response
        )

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"))

        val inntektsmeldinger = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

        assertThat(inntektsmeldinger[0].arbeidsgiverPrivat).isEqualTo("arbeidsgiverPrivat")
        assertThat(inntektsmeldinger[0].orgnummer).isNull()
        assertThat(inntektsmeldinger[0].aktorId).isEqualTo("aktorId_for_fnr")
    }

    @Throws(MessageNotWriteableException::class)
    private fun opprettKoemelding(arkivId: String): ActiveMQTextMessage {
        val message = ActiveMQTextMessage()
        message.text = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "  <ns5:forsendelsesinformasjon xmlns:ns5=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\" " +
                "    xmlns:ns2=\"http://nav.no/melding/virksomhet/dokumentforsendelse/v1\" " +
                "    xmlns:ns4=\"http://nav.no/dokmot/jms/reply\" " +
                "    xmlns:ns3=\"http://nav.no.dokmot/jms/viderebehandling\">" +
                "  <arkivId>" + arkivId + "</arkivId>" +
                "  <arkivsystem>JOARK</arkivsystem>" +
                "  <tema>SYK</tema>" +
                "  <behandlingstema>ab0061</behandlingstema>" +
                "</ns5:forsendelsesinformasjon>"

        return message
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

        `when`(journalV2.hentDokument(any())).thenReturn(
                dokumentResponse.response
        )

        val numThreads = 16
        produceParallelMessages(numThreads)

        runBlocking {
            verify<SakClient>(sakClient, times(1)).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
        verify<BehandleInngaaendeJournalV1>(behandleInngaaendeJournalV1, times(numThreads)).ferdigstillJournalfoering(
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

    @Test
    @Throws(Exception::class)
    fun behandlerInntektsmeldingerForFlerPersonerSamtidig() {
        val fnr = AtomicInteger()
        given(journalV2.hentDokument(any<HentDokumentRequest>())).willAnswer {
            build(fnr)
        }

        val numThreads = 16
        produceParallelMessages(numThreads)
        runBlocking {
            verify<SakClient>(sakClient, times(numThreads)).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
        verify<BehandleInngaaendeJournalV1>(behandleInngaaendeJournalV1, times(numThreads)).ferdigstillJournalfoering(
                any()
        )
    }

    @Throws(Exception::class)
    fun produceParallelMessages(numThreads: Int) {
        val countdown = CountDownLatch(numThreads)

        repeat(numThreads) {
            Thread {
                try {
                    inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"))
                } catch (e: MessageNotWriteableException) {
                    e.printStackTrace()
                }

                countdown.countDown()
            }.start()
        }
        countdown.await()
    }
}
