package no.nav.syfo.simba

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.syfo.behandling.OPPRETT_OPPGAVE_FORSINKELSE
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import java.util.UUID

internal class InntektsmeldingConsumerTest {
    lateinit var consumer: InntektsmeldingConsumer
    var props = joarkLocalProperties().toMap()
    val inntektsmeldingService: InntektsmeldingService = mockk(relaxed = true)
    val inntektsmeldingAivenProducer: InntektsmeldingAivenProducer = mockk(relaxed = true)
    var utsattOppgaveService: UtsattOppgaveService = mockk(relaxed = true)
    val pdlClient: PdlClient = mockk(relaxed = true)
    val topicName = "helsearbeidsgiver.inntektsmelding"
    val testNow: LocalDateTime = LocalDateTime.of(2025, 12, 5, 16, 45)

    @BeforeEach
    fun before() {
        consumer = InntektsmeldingConsumer(props, topicName, inntektsmeldingService, inntektsmeldingAivenProducer, utsattOppgaveService, pdlClient)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns testNow
    }

    @AfterEach
    fun after() {
        unmockkStatic(LocalDateTime::class)
    }

    @Test
    fun `behandle med IM type Forespurt oppretter utsattOppgave med timeout inkludert forsinkelse og legger til IM på topic`() {
        val timeoutNowPlusForsinkelse = LocalDateTime.now().plusHours(OPPRETT_OPPGAVE_FORSINKELSE)
        val im = lagInntektsmelding()

        consumer.behandle("123456789", im)

        verify {
            utsattOppgaveService.opprett(
                match {
                    it.timeout == timeoutNowPlusForsinkelse
                },
            )
        }

        verify(exactly = 1) {
            inntektsmeldingAivenProducer.sendTilTopicForVedtaksloesning(any())
            inntektsmeldingAivenProducer.sendTilTopicForBruker(any())
        }
    }

    @ParameterizedTest
    @MethodSource("inntektsmeldingTypesWithoutArbeidsforhold")
    fun `behandle med IM uten arbeidsforhold oppretter utsattOppgave med timeout now og legger ikke IM på topic for vedtaksloesning`(imType: Inntektsmelding.Type) {
        val timeoutNow = LocalDateTime.now()
        val im = lagInntektsmelding().copy(type = imType)

        consumer.behandle("123", im)

        verify {
            utsattOppgaveService.opprett(
                match {
                    it.timeout == timeoutNow
                },
            )
        }

        verify(exactly = 0) {
            inntektsmeldingAivenProducer.sendTilTopicForVedtaksloesning(any())
        }
        verify(exactly = 1) {
            inntektsmeldingAivenProducer.sendTilTopicForBruker(any())
        }
    }

    companion object {
        @JvmStatic
        fun inntektsmeldingTypesWithoutArbeidsforhold() =
            setOf(
                Inntektsmelding.Type.Fisker(UUID.randomUUID()),
                Inntektsmelding.Type.UtenArbeidsforhold(UUID.randomUUID()),
            )
    }
}
