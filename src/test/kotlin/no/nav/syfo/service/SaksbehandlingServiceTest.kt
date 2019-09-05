package no.nav.syfo.service

import any
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandleSakConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer
import no.nav.syfo.domain.*
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.repository.InntektsmeldingDAO
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.util.Arrays.asList
import java.util.Collections.emptyList

@RunWith(MockitoJUnitRunner::class)
class SaksbehandlingServiceTest {

    @Mock
    private lateinit var oppgavebehandlingConsumer: OppgavebehandlingConsumer
    @Mock
    private lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer
    @Mock
    private lateinit var behandleSakConsumer: BehandleSakConsumer
    @Mock
    private lateinit var aktoridConsumer: AktorConsumer
    @Mock
    private lateinit var inntektsmeldingDAO: InntektsmeldingDAO
    @Mock
    private lateinit var eksisterendeSakService: EksisterendeSakService
    @Mock
    private val metrikk: Metrikk? = null

    @InjectMocks
    private lateinit var saksbehandlingService: SaksbehandlingService

    @Before
    fun setup() {
        `when`(inntektsmeldingDAO.finnBehandledeInntektsmeldinger(any())).thenReturn(emptyList())
        `when`(aktoridConsumer.getAktorId(anyString())).thenReturn("aktorid")
        `when`(behandlendeEnhetConsumer.hentGeografiskTilknytning(anyString())).thenReturn(
            GeografiskTilknytningData(
                geografiskTilknytning = "Geografisktilknytning"
            )
        )
        `when`(behandlendeEnhetConsumer.hentBehandlendeEnhet(anyString())).thenReturn("behandlendeenhet1234")
        `when`(behandleSakConsumer.opprettSak("fnr")).thenReturn("opprettetSaksId")
        given(eksisterendeSakService.finnEksisterendeSak(any(), any(), any())).willReturn("saksId")
    }

    private fun lagInntektsmelding(): Inntektsmelding {
        return Inntektsmelding(
                journalStatus = JournalStatus.MIDLERTIDIG,
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                journalpostId = "journalpostId",
                arbeidsforholdId = null,
                arsakTilInnsending = "Ny",
                arbeidsgiverperioder = listOf(
                        Periode(
                                fom = LocalDate.of(2019, 1, 4),
                                tom = LocalDate.of(2019, 1, 20)
                        )
                )
        )
    }

    @Test
    fun returnererSaksIdOmSakFinnes() {
        val saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId")

        assertThat(saksId).isEqualTo("saksId")
    }

    @Test
    fun oppretterSakOmSakIkkeFinnes() {
        given(eksisterendeSakService.finnEksisterendeSak(anyString(), any(), any())).willReturn(null)

        val saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId")

        assertThat(saksId).isEqualTo("opprettetSaksId")
    }

    @Test
    fun oppretterOppgaveForSak() {
        saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId")

        verify<OppgavebehandlingConsumer>(oppgavebehandlingConsumer).opprettOppgave(
            anyString(),
            any<Oppgave>()
        )
    }

    @Test(expected = RuntimeException::class)
    @Throws(HentAktoerIdForIdentPersonIkkeFunnet::class)
    fun test() {
        `when`(aktoridConsumer.getAktorId(anyString())).thenThrow(HentAktoerIdForIdentPersonIkkeFunnet::class.java)

        saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId")
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmTomOverlapper() {
        `when`(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(
            listOf(
                InntektsmeldingMeta(
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

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId")
        assertThat(sakId).isEqualTo("1")
        verify<BehandleSakConsumer>(behandleSakConsumer, never()).opprettSak(anyString())
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOverlapper() {
        `when`(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(
            listOf(
                InntektsmeldingMeta(
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

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId")
        assertThat(sakId).isEqualTo("1")
        verify<BehandleSakConsumer>(behandleSakConsumer, never()).opprettSak(anyString())
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOgTomOverlapper() {
        `when`(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(
            listOf(
                InntektsmeldingMeta(
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

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId")
        assertThat(sakId).isEqualTo("1")
        verify<BehandleSakConsumer>(behandleSakConsumer, never()).opprettSak(anyString())
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
        )
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
                ), "aktor"
        )
        verify(eksisterendeSakService).finnEksisterendeSak("aktor", LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 14))
    }
}
