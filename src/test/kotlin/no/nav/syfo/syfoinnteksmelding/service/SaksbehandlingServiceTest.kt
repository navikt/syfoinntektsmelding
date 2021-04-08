package no.nav.syfo.syfoinnteksmelding.service

import io.ktor.util.*
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.SakResponse
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Collections.emptyList

class SaksbehandlingServiceTest {

    var oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    var behandlendeEnhetConsumer = mockk<BehandlendeEnhetConsumer>(relaxed = true)
    var aktoridConsumer = mockk<AktorConsumer>(relaxed = true)
    var inntektsmeldingService = mockk<InntektsmeldingService>(relaxed = true)
    var eksisterendeSakService = mockk<EksisterendeSakService>(relaxed = true)
    var sakClient = mockk<SakClient>(relaxed = true)
    val metrikk = mockk<Metrikk>(relaxed = true)
   // var utsattOppgaveService = mockk<UtsattOppgaveService>(relaxed = true)

    private var saksbehandlingService =
        SaksbehandlingService(eksisterendeSakService, inntektsmeldingService, sakClient, metrikk)


    @io.ktor.util.KtorExperimentalAPI
    @BeforeEach
    fun setup() {
        every { inntektsmeldingService.finnBehandledeInntektsmeldinger(any()) } returns emptyList()
        every { aktoridConsumer.getAktorId(any()) } returns "aktorid"
        every { eksisterendeSakService.finnEksisterendeSak(any(), any(), any()) } returns "saksId"
        every {
            runBlocking {
                sakClient.opprettSak(any(), any())
            }
        } returns SakResponse(
            id = 987,
            tema = "a",
            aktoerId = "123",
            applikasjon = "",
            fagsakNr = "123",
            opprettetAv = "",
            opprettetTidspunkt = ZonedDateTime.now(),
            orgnr = ""
        )
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
            arkivRefereranse = "ar123",
            førsteFraværsdag = LocalDate.now(),
            mottattDato = LocalDateTime.now()
        )
    }

    @Test
    fun returnererSaksIdOmSakFinnes() {
        val saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", "")

        assertThat(saksId).isEqualTo("saksId")
    }

    @Test
    fun oppretterSakOmSakIkkeFinnes() {
        every { eksisterendeSakService.finnEksisterendeSak(any(), any(), any()) } returns null

        val saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", "")

        assertThat(saksId).isEqualTo("987")
    }

    @io.ktor.util.KtorExperimentalAPI
    @Test
    fun oppretterIkkeOppgaveForSak() {
        saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", "")

        runBlocking {
            coVerify(exactly = 0) { oppgaveClient.opprettOppgave(any(), any(), any(), any(), any()) }
        }
    }

    @KtorExperimentalAPI
    //@Disabled
    @Test
    fun test() {
        every { aktoridConsumer.getAktorId(any()) } throws HentAktoerIdForIdentPersonIkkeFunnet()
        Assertions.assertThrows(HentAktoerIdForIdentPersonIkkeFunnet::class.java) {
            saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", "")
        }
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmTomOverlapper() {
        every { inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId") } returns
            listOf(
                lagInntektsmelding2(
                    aktorId = "aktorId",
                    journalpostId = "id",
                    sakId = "1",
                    arbeidsgiverperioder = listOf(
                        Periode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 1, 20)
                        )
                    )
                )
            )

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", "")
        assertThat(sakId).isEqualTo("1")
        runBlocking {
            coVerify(exactly = 0) { sakClient.opprettSak(any(), any()) }
        }
    }

    private fun lagInntektsmelding2(
        aktorId: String,
        journalpostId: String,
        sakId: String,
        arbeidsgiverperioder: List<Periode>
    ): Inntektsmelding {
        return Inntektsmelding(
            id = "ID",
            fnr = "fnr",
            arbeidsgiverOrgnummer = "orgnummer",
            arbeidsforholdId = null,
            journalpostId = journalpostId,
            arsakTilInnsending = "Ny",
            journalStatus = JournalStatus.MIDLERTIDIG,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arkivRefereranse = "AR",
            førsteFraværsdag = LocalDate.now(),
            mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay(),
            sakId = sakId,
            aktorId = aktorId
        )
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOverlapper() {
        every { inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId") } returns
            listOf(
                lagInntektsmelding2(
                    aktorId = "aktorId",
                    journalpostId = "journalPostId",
                    sakId = "1",
                    arbeidsgiverperioder = listOf(
                        Periode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 1, 24)
                        )
                    )
                )
            )


        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", "")
        assertThat(sakId).isEqualTo("1")
        runBlocking {
            coVerify(exactly = 0) { sakClient.opprettSak(any(), any()) }
        }
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOgTomOverlapper() {
        every { inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId") } returns
            listOf(
                lagInntektsmelding2(
                    aktorId = "aktorId",
                    journalpostId = "journalPostId",
                    sakId = "1",
                    arbeidsgiverperioder = listOf(
                        Periode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 1, 20)
                        )
                    )
                )
            )

        val sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId", "")
        assertThat(sakId).isEqualTo("1")
        runBlocking {
            coVerify(exactly = 0) { sakClient.opprettSak(any(), any()) }
        }
    }

    @Test
    fun kallerHentSakMedFomTomFraArbeidsgiverperiodeIInntektsmeldingMedEnPeriode() {
        saksbehandlingService.behandleInntektsmelding(
            lagInntektsmelding()
                .copy(
                    arbeidsgiverperioder = listOf(
                        Periode(
                            fom = LocalDate.of(2019, 6, 6),
                            tom = LocalDate.of(2019, 6, 10)
                        )
                    )
                ), "aktor", "")

        verify {
            eksisterendeSakService.finnEksisterendeSak(
                "aktor",
                LocalDate.of(2019, 6, 6),
                LocalDate.of(2019, 6, 10)
            )
        }
    }

    @Test
    fun kallerHentSakMedTidligsteFomOgSenesteTomFraArbeidsgiverperiodeIInntektsmeldingFlerePerioder() {
        saksbehandlingService.behandleInntektsmelding(
            lagInntektsmelding()
                .copy(
                    arbeidsgiverperioder = listOf(
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
                ), "aktor", "")
        verify {
            eksisterendeSakService.finnEksisterendeSak(
                "aktor",
                LocalDate.of(2019, 6, 1),
                LocalDate.of(2019, 6, 14)
            )
        }
    }
}
