package no.nav.syfo.consumer.mq

import no.nav.syfo.LocalApplication
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandleSakConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumerTest
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.repository.InntektsmeldingDAO
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.BehandleInngaaendeJournalV1
import no.nav.tjeneste.virksomhet.journal.v2.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.WSHentDokumentRequest
import no.nav.tjeneste.virksomhet.journal.v2.WSHentDokumentResponse
import org.apache.activemq.command.ActiveMQTextMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
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
    private val inngaaendeJournalConsumer: InngaaendeJournalConsumer? = null

    @MockBean
    private lateinit var journalV2: JournalV2

    @MockBean
    private lateinit var aktorConsumer: AktorConsumer

    @MockBean
    private lateinit var eksisterendeSakService: EksisterendeSakService

    @MockBean
    private val behandleSakConsumer: BehandleSakConsumer? = null

    @MockBean
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer? = null

    @MockBean
    private val oppgavebehandlingConsumer: OppgavebehandlingConsumer? = null

    @MockBean
    private val behandleInngaaendeJournalV1: BehandleInngaaendeJournalV1? = null

    @MockBean
    private val metrikk: Metrikk? = null

    @Inject
    private val inntektsmeldingConsumer: InntektsmeldingConsumer? = null

    @Inject
    private val inntektsmeldingDAO: InntektsmeldingDAO? = null

    @Inject
    private val jdbcTemplate: JdbcTemplate? = null

    private var saksIdteller = 0

    @Before
    fun setup() {
        jdbcTemplate!!.update("DELETE FROM ARBEIDSGIVERPERIODE")
        jdbcTemplate.update("DELETE FROM INNTEKTSMELDING")
        saksIdteller = 0

        val inngaaendeJournal = inngaaendeJournal("arkivId")

        `when`(behandleSakConsumer!!.opprettSak(ArgumentMatchers.anyString())).thenAnswer { invocation -> "saksId" + saksIdteller++ }

        `when`(inngaaendeJournalConsumer!!.hentDokumentId("arkivId")).thenReturn(inngaaendeJournal)

        given(aktorConsumer.getAktorId(ArgumentMatchers.anyString())).willAnswer { "aktorId_for_" + it.getArgument(0) }

        given(eksisterendeSakService.finnEksisterendeSak(ArgumentMatchers.anyString(), any(), any())).willReturn(null)

        `when`(behandlendeEnhetConsumer!!.hentBehandlendeEnhet(ArgumentMatchers.anyString())).thenReturn("enhet")
        `when`(behandlendeEnhetConsumer.hentGeografiskTilknytning(ArgumentMatchers.anyString())).thenReturn(
            GeografiskTilknytningData(geografiskTilknytning = "tilknytning", diskresjonskode = "")
        )
    }

    private fun inngaaendeJournal(arkivId: String): InngaaendeJournal {
        return InngaaendeJournal(
            dokumentId = arkivId,
            status = JournalStatus.MIDLERTIDIG
        )
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun gjenbrukerSaksIdHvisViFarToOverlappendeInntektsmeldinger() {
        `when`(inngaaendeJournalConsumer!!.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"))

        `when`(journalV2!!.hentDokument(any())).thenReturn(
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16)))
                ).toByteArray()
            ),
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(Periode(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 16)))
                ).toByteArray()
            )
        )

        inntektsmeldingConsumer!!.listen(opprettKoemelding("arkivId1"))
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"))

        val inntektsmeldingMetas = inntektsmeldingDAO!!.finnBehandledeInntektsmeldinger("aktorId_for_fnr")
        assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        assertThat(inntektsmeldingMetas[0].sakId).isEqualTo(inntektsmeldingMetas[1].sakId)

        verify<BehandleSakConsumer>(behandleSakConsumer, times(1)).opprettSak("fnr")
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun gjenbrukerIkkeSaksIdHvisViFarToInntektsmeldingerSomIkkeOverlapper() {
        `when`(inngaaendeJournalConsumer!!.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"))

        `when`(journalV2!!.hentDokument(any())).thenReturn(
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16)))
                ).toByteArray()
            ),
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(Periode(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16)))
                ).toByteArray()
            )
        )

        inntektsmeldingConsumer!!.listen(opprettKoemelding("arkivId1"))
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"))

        val inntektsmeldingMetas = inntektsmeldingDAO!!.finnBehandledeInntektsmeldinger("aktorId_for_fnr")
        assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("saksId0")
        assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("saksId1")

        verify<BehandleSakConsumer>(behandleSakConsumer, times(2)).opprettSak("fnr")
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun brukerSaksIdFraSykeforloepOmViIkkeHarOverlappendeInntektsmelding() {
        given(eksisterendeSakService.finnEksisterendeSak("aktorId_for_fnr", null, null)).willReturn(null, "syfosak")

        `when`(inngaaendeJournalConsumer!!.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"))
        `when`(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"))

        `when`(journalV2!!.hentDokument(any())).thenReturn(
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16)))
                ).toByteArray()
            ),
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(Periode(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 2, 16)))
                ).toByteArray()
            )
        )

        inntektsmeldingConsumer!!.listen(opprettKoemelding("arkivId1"))
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"))

        val inntektsmeldingMetas = inntektsmeldingDAO!!.finnBehandledeInntektsmeldinger("aktorId_for_fnr")
        assertThat(inntektsmeldingMetas.size).isEqualTo(2)
        assertThat(inntektsmeldingMetas[0].sakId).isNotEqualTo(inntektsmeldingMetas[1].sakId)

        assertThat(inntektsmeldingMetas[0].sakId).isEqualTo("saksId0")
        assertThat(inntektsmeldingMetas[1].sakId).isEqualTo("syfosak")

        verify<BehandleSakConsumer>(behandleSakConsumer, times(1)).opprettSak("fnr")
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun mottarInntektsmeldingUtenArbeidsgiverperioder() {
        `when`(journalV2!!.hentDokument(any())).thenReturn(
            WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(emptyList<Periode>()).toByteArray())
        )

        inntektsmeldingConsumer!!.listen(opprettKoemelding("arkivId"))

        val inntektsmeldinger = inntektsmeldingDAO!!.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

        assertThat(inntektsmeldinger[0].arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    @Throws(
        MessageNotWriteableException::class,
        HentDokumentSikkerhetsbegrensning::class,
        HentDokumentDokumentIkkeFunnet::class
    )
    fun mottarInntektsmeldingMedFlerePerioder() {
        `when`(journalV2!!.hentDokument(any())).thenReturn(
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    asList(
                        Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 12)),
                        Periode(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 14))
                    )
                ).toByteArray()
            )
        )

        inntektsmeldingConsumer!!.listen(opprettKoemelding("arkivId"))

        val inntektsmeldinger = inntektsmeldingDAO!!.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

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
        `when`(journalV2!!.hentDokument(any())).thenReturn(
            WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiverPrivat().toByteArray())
        )

        inntektsmeldingConsumer!!.listen(opprettKoemelding("arkivId"))

        val inntektsmeldinger = inntektsmeldingDAO!!.finnBehandledeInntektsmeldinger("aktorId_for_fnr")

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
        `when`(journalV2!!.hentDokument(any())).thenReturn(
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(20))
                    )
                ).toByteArray()
            )
        )

        val numThreads = 16
        produceParallelMessages(numThreads)

        verify<BehandleSakConsumer>(behandleSakConsumer, times(1)).opprettSak(ArgumentMatchers.anyString())
        verify<BehandleInngaaendeJournalV1>(behandleInngaaendeJournalV1, times(numThreads)).ferdigstillJournalfoering(
            any()
        )
    }

    @Test
    @Throws(Exception::class)
    fun behandlerInntektsmeldingerForFlerPersonerSamtidig() {
        val fnr = AtomicInteger()
        given(journalV2.hentDokument(any<WSHentDokumentRequest>())).willAnswer {
            WSHentDokumentResponse().withDokument(
                JournalConsumerTest.inntektsmeldingArbeidsgiver(
                    listOf(
                        Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(20)
                        )
                    ),
                    "fnr" + fnr.incrementAndGet()
                ).toByteArray()
            )
        }

        val numThreads = 16
        produceParallelMessages(numThreads)

        verify<BehandleSakConsumer>(behandleSakConsumer, times(numThreads)).opprettSak(ArgumentMatchers.anyString())
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
                    inntektsmeldingConsumer!!.listen(opprettKoemelding("arkivId"))
                } catch (e: MessageNotWriteableException) {
                    e.printStackTrace()
                }

                countdown.countDown()
            }.start()
        }
        countdown.await()
    }
}
