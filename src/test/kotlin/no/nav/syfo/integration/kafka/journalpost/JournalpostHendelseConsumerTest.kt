package no.nav.syfo.integration.kafka.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
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

internal class JournalpostHendelseConsumerTest {

    var duplikatRepository: DuplikatRepository = mockk(relaxed = true)
    var bakgrunnsjobbRepo: BakgrunnsjobbRepository = mockk(relaxed = true)
    var om: ObjectMapper = mockk(relaxed = true)

    var props = joarkLocalProperties().toMap()
    val topicName = "topic"
    val dto = InngaaendeJournalpostDTO("abc", 1, "", 123, "MOTTATT", "", "SYK", "ALTINN", "", "")

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
    fun skal_ignorere_journalposter_som_ikke_er_inntektsmelding() {
        every {
            duplikatRepository.findByHendelsesId(any())
        } returns true
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        assertEquals(-1, consumer.processHendelse(dto.copy(temaNytt = "")))
    }

    @Test
    fun skal_ignorere_duplikate_inntektsmeldinger() {
        every {
            duplikatRepository.findByHendelsesId(any())
        } returns true
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        assertEquals(0, consumer.processHendelse(dto))
    }

    @Test
    fun skal_lagre_inntektsmeldinger() {
        every {
            duplikatRepository.findByHendelsesId(any())
        } returns false
        val consumer = JournalpostHendelseConsumer(props, topicName, duplikatRepository, bakgrunnsjobbRepo, om)
        assertEquals(1, consumer.processHendelse(dto))
    }

    @Test
    fun skal_sjekke_om_inntektsmelding() {
        assertTrue(isInntektsmelding(dto))
        assertFalse(isInntektsmelding(dto.copy(journalpostStatus = "IKKE_MOTTATT")))
        assertFalse(isInntektsmelding(dto.copy(temaNytt = "IKKE_SYK")))
        assertFalse(isInntektsmelding(dto.copy(mottaksKanal = "IKKE_ALTINN")))
    }
}
