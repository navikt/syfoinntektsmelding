package no.nav.syfo.integration.kafka.journalpost

import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import org.apache.avro.generic.GenericRecord

fun mapJournalpostHendelse(record: GenericRecord): InngaaendeJournalpostDTO {
    return InngaaendeJournalpostDTO(
        record.get("hendelsesId") as String,
        record.get("versjon") as Int,
        record.get("hendelsesType") as String,
        record.get("journalpostId") as Long,
        record.get("journalpostStatus") as String,
        record.get("temaGammelt") as String,
        record.get("temaNytt") as String,
        record.get("mottaksKanal") as String,
        record.get("kanalReferanseId") as String,
        record.get("behandlingstema") as String,
    )
}
