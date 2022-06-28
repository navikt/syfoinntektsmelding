package no.nav.syfo.client.dokarkiv

fun mapFeilregistrertRequest(
    fnr: String,
    arbeidsgiverNr: String,
    arbeidsgiverNavn: String,
    isArbeidsgiverFnr: Boolean,
    dokumentId: String
): OppdaterJournalpostRequest {
    return OppdaterJournalpostRequest(
        bruker = Bruker(
            fnr,
            "FNR"
        ),
        avsenderMottaker = AvsenderMottaker(
            arbeidsgiverNr,
            if (isArbeidsgiverFnr) {
                "FNR"
            } else {
                "ORGNR"
            },
            arbeidsgiverNavn
        ),
        sak = Sak("GENERELL_SAK"),
        tema = "SYK",
        dokumenter = listOf(Dokument(dokumentId, "Inntektsmelding duplikat"))
    )
}
