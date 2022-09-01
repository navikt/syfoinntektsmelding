package no.nav.syfo.integration.kafka.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.repository.DuplikatRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JournalpostHendelseConsumerTest {

    var duplikatRepository: DuplikatRepository = mockk(relaxed = true)
    var bakgrunnsjobbRepo: BakgrunnsjobbRepository = mockk(relaxed = true)
    var om: ObjectMapper = mockk(relaxed = true)

    var props = joarkLocalProperties().toMap()
    val topicName = "topic"
    val GYLDIG_INNTEKTSMELDING = InngaaendeJournalpostDTO("abc", 1, "", 111, "MOTTATT", "", "SYK", "ALTINN", "", "")
    val DUPLIKAT_INNTEKTSMELDING = GYLDIG_INNTEKTSMELDING.copy(journalpostId = 222)
    val IKKE_INNTEKTSMELDING = InngaaendeJournalpostDTO("abc", 1, "", 333, "IKKE_MOTTATT", "", "IKKE_SYK", "IKKE_ALTINN", "", "")

    @Test
    fun isready_skal_gi_feilmelding_før_oppstart() {
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runReadynessCheck()
            }
        }
    }

    @Test
    fun isready_skal_ikke_gi_feilmelding_etter_oppstart() {
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        consumer.setIsReady(true)
        runBlocking {
            consumer.runReadynessCheck()
        }
    }

    @Test
    fun liveness_skal_gi_feilmelding_når_feil_oppstår() {
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        consumer.setIsError(true)
        assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runLivenessCheck()
            }
        }
    }

    @Test
    fun liveness_skal_ikke_gi_feilmelding_når_alt_virker() {
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        consumer.setIsError(false)
        runBlocking {
            consumer.runLivenessCheck()
        }
    }

    @Test
    fun skal_lagre_inntektsmelding() {
        every {
            duplikatRepository.findByHendelsesId(any())
        } returns false
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        consumer.processHendelse(GYLDIG_INNTEKTSMELDING)
        verify(exactly = 1) { bakgrunnsjobbRepo.save(any()) }
    }

    @Test
    fun skal_gjenkjenne_duplikat() {
        every {
            duplikatRepository.findByHendelsesId(any())
        } returns true
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        assertEquals(JournalpostStatus.Duplikat, consumer.findStatus(DUPLIKAT_INNTEKTSMELDING))
    }

    @Test
    fun skal_gjenkjenne_nye() {
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        assertEquals(JournalpostStatus.Ny, consumer.findStatus(GYLDIG_INNTEKTSMELDING))
    }

    @Test
    fun skal_gjenkjenne_ikke_inntektsmeldinger() {
        every {
            duplikatRepository.findByHendelsesId(any())
        } returns false
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        assertEquals(JournalpostStatus.IkkeInntektsmelding, consumer.findStatus(IKKE_INNTEKTSMELDING))
    }

    @Test
    fun skal_sjekke_om_inntektsmelding() {
        assertTrue(isInntektsmelding(GYLDIG_INNTEKTSMELDING))
        assertFalse(isInntektsmelding(GYLDIG_INNTEKTSMELDING.copy(journalpostStatus = "IKKE_MOTTATT")))
        assertFalse(isInntektsmelding(GYLDIG_INNTEKTSMELDING.copy(temaNytt = "IKKE_SYK")))
        assertFalse(isInntektsmelding(GYLDIG_INNTEKTSMELDING.copy(mottaksKanal = "IKKE_ALTINN")))
    }
}
