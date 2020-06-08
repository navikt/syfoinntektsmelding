package no.nav.syfo.kafkamottak

data class InngaaendeJournalpostDTO(
    val hendelsesId: String,
    val versjon: Int,
    val hendelsesType: String,
    val journalpostId: Long,
    val journalpostStatus: String,
    val temaGammelt: String,
    val temaNytt: String,
    val mottaksKanal: String,
    val kanalReferanseId: String
)
