package no.nav.syfo.service

import any
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.SakResponse
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.domain.*
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Arrays.asList
import java.util.Collections.emptyList

@RunWith(MockitoJUnitRunner::class)
class SaksbehandlingServiceTest {

    @Mock
    private lateinit var oppgaveClient: OppgaveClient

    @Mock
    private lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer
    @Mock
    private lateinit var aktoridConsumer: AktorConsumer
    @Mock
    private lateinit var inntektsmeldingService: InntektsmeldingService
    @Mock
    private lateinit var eksisterendeSakService: EksisterendeSakService
    @Mock
    private lateinit var sakClient: SakClient
    @Mock
    private val metrikk: Metrikk? = null
    @Mock
    private lateinit var utsattOppgaveService: UtsattOppgaveService


    @InjectMocks
    private lateinit var saksbehandlingService: SaksbehandlingService

    @io.ktor.util.KtorExperimentalAPI
    @Before
    fun setup() {
        `when`(inntektsmeldingService.finnBehandledeInntektsmeldinger(any())).thenReturn(emptyList())
        `when`(aktoridConsumer.getAktorId(anyString())).thenReturn("aktorid")
        given(eksisterendeSakService.finnEksisterendeSak(any(), any(), any())).willReturn("saksId")
        given(runBlocking{sakClient.opprettSak(any(), any())}).willReturn(SakResponse(id = 987, tema = "a", aktoerId = "123", applikasjon = "", fagsakNr = "123", opprettetAv = "", opprettetTidspunkt = ZonedDateTime.now(), orgnr = ""))
    }

    private fun lagInntektsmelding(): Inntektsmelding {
        return Inntektsmelding(
                id = "ID",
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsforholdId = null,
                journalpostId = "journalpostId",
                arsakTilInnsending = "Ny",
                journalStatus = JournalStatus.MIDLERTIDIG,
                arbeidsgiverperioder = listOf(
                        Periode(
                                fom = LocalDate.of(2019, 1, 4),
                                tom = LocalDate.of(2019, 1, 20)
                        )
                ),
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDateTime.now(),
                arkivRefereranse = "ar123"
        )
    }

    @Test
    fun returnererSaksIdOmSakFinnes() {
        val saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", anyString())

        assertThat(saksId).isEqualTo("saksId")
    }

    @Test
    fun oppretterSakOmSakIkkeFinnes() {
        given(eksisterendeSakService.finnEksisterendeSak(anyString(), any(), any())).willReturn(null)

        val saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", anyString())

        assertThat(saksId).isEqualTo("987")
    }

    @io.ktor.util.KtorExperimentalAPI
    @Test
    fun oppretterIkkeOppgaveForSak() {
        saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", anyString())

        runBlocking {
            verify(oppgaveClient, never()).opprettOppgave(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean()
            )
        }
    }

    @Test(expected = RuntimeException::class)
    @Throws(HentAktoerIdForIdentPersonIkkeFunnet::class)
    fun test() {
        `when`(aktoridConsumer.getAktorId(anyString())).thenThrow(HentAktoerIdForIdentPersonIkkeFunnet::class.java)

        saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", anyString())
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmTomOverlapper() {
        `when`(inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(
                listOf(
                        lagInntektsmelding2(
                                aktorId = "aktorId",
                                journalpostId = "id",
                                sakId = "1",
                                arbeidsgiverperioder = asList(
                                        Periode(
                                                fom = LocalDate.of(2019, 1, 1),
                                                tom = LocalDate.of(2019, 1, 20)
                                        )
                                )
                        )
                )
        )

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", anyString())
        assertThat(sakId).isEqualTo("1")
        runBlocking {
            verify<SakClient>(sakClient, never()).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
    }

    private fun lagInntektsmelding2(aktorId: String, journalpostId: String, sakId: String, arbeidsgiverperioder: List<Periode>): Inntektsmelding {
        return Inntektsmelding(
                aktorId = aktorId,
                id = "ID",
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsforholdId = null,
                journalpostId = journalpostId,
                arsakTilInnsending = "Ny",
                sakId = sakId,
                arbeidsgiverperioder = arbeidsgiverperioder,
                arkivRefereranse = "AR",
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay(),
                journalStatus = JournalStatus.MIDLERTIDIG,
                førsteFraværsdag = LocalDate.now()
        )
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOverlapper() {
        `when`(inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(
                listOf(
                        lagInntektsmelding2(
                                aktorId = "aktorId",
                                journalpostId = "journalPostId",
                                sakId = "1",
                                arbeidsgiverperioder = asList(
                                        Periode(
                                                fom = LocalDate.of(2019, 1, 1),
                                                tom = LocalDate.of(2019, 1, 24)
                                        )
                                )
                        )
                )
        )

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", anyString())
        assertThat(sakId).isEqualTo("1")
        runBlocking {
            verify<SakClient>(sakClient, never()).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOgTomOverlapper() {
        `when`(inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(
                listOf(
                        lagInntektsmelding2(
                                aktorId = "aktorId",
                                journalpostId = "journalPostId",
                                sakId = "1",
                                arbeidsgiverperioder = asList(
                                        Periode(
                                                fom = LocalDate.of(2019, 1, 1),
                                                tom = LocalDate.of(2019, 1, 20)
                                        )
                                )
                        )
                )
        )

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", anyString())
        assertThat(sakId).isEqualTo("1")
        runBlocking {
            verify<SakClient>(sakClient, never()).opprettSak(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        }
    }

    @Test
    fun kallerHentSakMedFomTomFraArbeidsgiverperiodeIInntektsmeldingMedEnPeriode() {
        saksbehandlingService.behandleInntektsmelding(
                lagInntektsmelding()
                        .copy(
                                arbeidsgiverperioder = asList(
                                        Periode(
                                                fom = LocalDate.of(2019, 6, 6),
                                                tom = LocalDate.of(2019, 6, 10)
                                        )
                                )
                        ), "aktor"
                , anyString())
        verify(eksisterendeSakService).finnEksisterendeSak("aktor", LocalDate.of(2019, 6, 6), LocalDate.of(2019, 6, 10))
    }

    @Test
    fun kallerHentSakMedTidligsteFomOgSenesteTomFraArbeidsgiverperiodeIInntektsmeldingFlerePerioder() {
        saksbehandlingService.behandleInntektsmelding(
                lagInntektsmelding()
                        .copy(
                                arbeidsgiverperioder = asList(
                                        Periode(
                                                fom = LocalDate.of(2019, 6, 6),
                                                tom = LocalDate.of(2019, 6, 10)
                                        ),
                                        Periode(
                                                fom = LocalDate.of(2019, 6, 1),
                                                tom = LocalDate.of(2019, 6, 5)
                                        ),
                                        Periode(
                                                fom = LocalDate.of(2019, 6, 11),
                                                tom = LocalDate.of(2019, 6, 14)
                                        )
                                )
                        ), "aktor", anyString()
        )
        verify(eksisterendeSakService).finnEksisterendeSak("aktor", LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 14))
    }
}
