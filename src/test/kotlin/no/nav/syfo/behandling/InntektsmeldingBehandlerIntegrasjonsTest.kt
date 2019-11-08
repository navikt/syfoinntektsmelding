package no.nav.syfo.behandling

import any
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.test.LocalApplication
import no.nav.syfo.util.Metrikk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.matches
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.time.LocalDateTime


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@EnableJpaRepositories("no.nav.syfo")
@EntityScan(basePackages = ["no.nav.syfo.dto"])
@ActiveProfiles("test")
open class InntektsmeldingBehandlerIntegrasjonsTest {

    @Mock
    lateinit var journalpostService: JournalpostService
    @Mock
    lateinit var saksbehandlingService: SaksbehandlingService
    @Mock
    lateinit var metrikk: Metrikk
    @Mock
    lateinit var aktorConsumer: AktorConsumer
    @Mock
    lateinit var inntektsmeldingProducer: InntektsmeldingProducer

    @Autowired
    lateinit var inntektsmeldingRepository: InntektsmeldingRepository

    lateinit var inntektsmeldingService: InntektsmeldingService

    lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @Before
    fun init() {
    }

    @Before
    fun setup() {
        inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
        inntektsmeldingBehandler = InntektsmeldingBehandler(journalpostService, saksbehandlingService, metrikk, inntektsmeldingService, aktorConsumer, inntektsmeldingProducer)
        MockitoAnnotations.initMocks(inntektsmeldingBehandler)
    }

    @Test
    fun gjenbruker_SaksIdHvisViFar_ToOverlappendeInntektsmeldinger() {
        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
            Inntektsmelding(
                id="abc",
                fnr = "fnr",
                journalpostId = "arkivId",
                arsakTilInnsending = "asd",
                journalStatus = JournalStatus.MIDLERTIDIG,
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.of(2019,1,1),
                mottattDato = LocalDateTime.now()
            )
        )
        `when`(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId") // inntektsmelding.fnr
        `when`(saksbehandlingService.behandleInntektsmelding(any(), matches("aktorId"), matches("AR-123"))).thenReturn("saksId")

        val dto = inntektsmeldingBehandler.behandle("arkivId", "AR-123")
        assertNotNull(dto?.uuid)
        assertEquals("arkivId", dto?.journalpostId)
        assertEquals("aktorId", dto?.aktorId)
        assertEquals("saksId", dto?.sakId)
//        assertEquals("fnr", )
    }

    @Test
    fun gjenbruker_Ikke_SaksIdHvisViFarToInntektsmeldingerSomIkkeOverlapper() {
    }

    @Test
    fun brukerSaksIdFraSykeforloepOmViIkkeHarOverlappendeInntektsmelding() {
    }

    @Test
    fun uten_arbeidsgiverperioder() {

    }

    @Test
    fun med_FlerePerioder() {
    }

    @Test
    fun med_PrivatArbeidsgiver() {
    }

    @Test
    fun somEnSakMedVedLikPeriode() {
    }

    @Test
    fun forFlerPersonerSamtidig() {
    }

}
