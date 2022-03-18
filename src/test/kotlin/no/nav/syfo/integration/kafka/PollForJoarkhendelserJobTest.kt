package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PollForJoarkhendelserJobTest {

    val gyldige = listOf(
        mockJournalpost("hendelse-0", "SYK", "ALTINN", "MOTTATT"),
    )

    val duplikater = listOf(
        mockJournalpost("hendelse-finnes", "SYK", "ALTINN", "MOTTATT"),
    )

    val ugyldige = listOf(
        mockJournalpost("hendelse-1", "FOR", "ALTINN", "MOTTATT"),
        mockJournalpost("hendelse-2", "SYK", "ALTINN", "MIDLERTIDIG"),
        mockJournalpost("hendelse-3", "OMS", "SYSTEM_A", "MOTTATT"),
        mockJournalpost("hendelse-4", "SYK", "SYSTEM_B", "MIDLERTIDIG")
    )

    val kafkaProvider = mockk<JoarkHendelseKafkaClient>(relaxed = true)
    val bakgrunnsjobbRepo = mockk<BakgrunnsjobbRepository>(relaxed = true)
    val duplikatRepository = mockk<DuplikatRepository>(relaxed = true)
    val objectMapper = mockk<ObjectMapper>(relaxed = true)
    val job = PollForJoarkhendelserJob(kafkaProvider, bakgrunnsjobbRepo, duplikatRepository, objectMapper)

    @Test
    fun skal_akseptere_gyldig_journalpost_som_ikke_er_duplikat() {
        every { duplikatRepository.findByHendelsesId(any()) } returns false
        assertTrue(job.isInntektsmelding(gyldige.first()))
    }

    @Test
    fun skal_ignorere_ugyldige_journalposter() {
        ugyldige.forEach {
            assertFalse(job.isInntektsmelding(it))
        }
    }

    @Test
    fun skal_godta_gyldige_journalposter() {
        assertTrue(job.isInntektsmelding(gyldige.first()))
    }

    @Test
    fun skal_ignorere_tidligere_behandlede_journalposter() {
        every { duplikatRepository.findByHendelsesId(any()) } returns true
        assertTrue(job.isDuplicate(duplikater.first()))
    }

    fun mockJournalpost(hendelseId: String, tema: String, kanal: String, status: String): InngaaendeJournalpostDTO {
        return InngaaendeJournalpostDTO(
            hendelseId,
            1,
            "hendelsesType",
            1234,
            status,
            "tema",
            tema,
            kanal,
            "kanalReferanseId-1",
            "behandlingstema-1"
        )
    }
}
