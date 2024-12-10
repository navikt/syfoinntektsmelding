package no.nav.syfo.client.dokarkiv

fun mapFeilregistrertRequest(
    fnr: String,
    dokumentId: String,
): OppdaterJournalpostRequest {
    return OppdaterJournalpostRequest(
        bruker =
            Bruker(
                fnr,
                "FNR",
            ),
        sak = Sak("GENERELL_SAK"),
        tema = "SYK",
        dokumenter = listOf(Dokument(dokumentId, "Inntektsmelding duplikat")),
    )
}
