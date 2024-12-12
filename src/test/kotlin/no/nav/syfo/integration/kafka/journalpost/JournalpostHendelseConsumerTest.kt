package no.nav.syfo.integration.kafka.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JournalpostHendelseConsumerTest {
    var bakgrunnsjobbRepo: BakgrunnsjobbRepository = mockk(relaxed = true)
    var om: ObjectMapper = mockk(relaxed = true)
    var props = joarkLocalProperties().toMap()
    val topicName = "topic"
    lateinit var consumer: JournalpostHendelseConsumer
    val gyldigInntektsmelding = InngaaendeJournalpostDTO("abc", 1, "JournalpostMottatt", 111, "MOTTATT", "", "SYK", "ALTINN", "", "")
    val ikkeInntektsmelding =
        InngaaendeJournalpostDTO("abc", 1, "JournalpostMottatt", 333, "IKKE_MOTTATT", "", "IKKE_SYK", "IKKE_ALTINN", "", "")
    val feilHendelseType = InngaaendeJournalpostDTO("abc", 1, "TemaEndret", 333, "MOTTATT", "", "SYK", "ALTINN", "", "")

    @BeforeEach
    fun before() {
        consumer = JournalpostHendelseConsumer(props, topicName, bakgrunnsjobbRepo, om)
    }

    @Test
    fun isready_skal_gi_feilmelding_før_oppstart() {
        assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runReadynessCheck()
            }
        }
    }

    @Test
    fun isready_skal_ikke_gi_feilmelding_etter_oppstart() {
        consumer.setIsReady(true)
        runBlocking {
            consumer.runReadynessCheck()
        }
    }

    @Test
    fun liveness_skal_gi_feilmelding_når_feil_oppstår() {
        consumer.setIsError(true)
        assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runLivenessCheck()
            }
        }
    }

    @Test
    fun liveness_skal_ikke_gi_feilmelding_når_alt_virker() {
        consumer.setIsError(false)
        runBlocking {
            consumer.runLivenessCheck()
        }
    }

    @Test
    fun skal_lagre_inntektsmelding() {
        consumer.processHendelse(gyldigInntektsmelding)
        verify(exactly = 1) { bakgrunnsjobbRepo.save(any()) }
    }

    @Test
    fun skal_gjenkjenne_nye() {
        assertEquals(JournalpostStatus.Ny, consumer.findStatus(gyldigInntektsmelding))
    }

    @Test
    fun skal_gjenkjenne_ikke_inntektsmeldinger() {
        assertEquals(JournalpostStatus.IkkeInntektsmelding, consumer.findStatus(ikkeInntektsmelding))
        verify(exactly = 0) { bakgrunnsjobbRepo.save(any()) }
    }

    @Test
    fun skal_gjenkjenne_feil_hendelser() {
        assertEquals(JournalpostStatus.FeilHendelseType, consumer.findStatus(feilHendelseType))
        verify(exactly = 0) { bakgrunnsjobbRepo.save(any()) }
    }

    @Test
    fun skal_sjekke_om_inntektsmelding() {
        assertTrue(isInntektsmelding(gyldigInntektsmelding))
        assertFalse(isInntektsmelding(gyldigInntektsmelding.copy(journalpostStatus = "IKKE_MOTTATT")))
        assertFalse(isInntektsmelding(gyldigInntektsmelding.copy(temaNytt = "IKKE_SYK")))
        assertFalse(isInntektsmelding(gyldigInntektsmelding.copy(mottaksKanal = "IKKE_ALTINN")))
    }
}
