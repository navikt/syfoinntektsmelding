package no.nav.syfo.util

import io.prometheus.client.Counter
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding



class Metrikk {

    private val METRICS_NS = "spinn"

    private fun proseseringsMetrikker(metricName: String, metricHelpText: String): Counter {
        return Counter.build()
            .namespace(METRICS_NS)
            .name(metricName)
            .labelNames("hendelse")
            .help(metricHelpText)
            .register()
    }

    fun tellInntektsmeldingerMottatt(inntektsmelding: Inntektsmelding) {
        val harArbeidsforholdId = inntektsmelding.arbeidsforholdId != null
        val metricHelpText = """
                "harArbeidsforholdId", ${if (harArbeidsforholdId) "J" else "N"},
                "arsakTilSending", ${inntektsmelding.arsakTilInnsending}
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_mottatt", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingerJournalfort() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_journalfort", metricHelpText).labels("info").inc()

    }

    fun tellOverlappendeInntektsmelding() {
        val metricHelpText = """
               "kobling", $OVERLAPPENDE
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_kobling", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingSaksIdFraSyfo() {
        val metricHelpText = """
                "kobling", $SAK_FRA_SYFO
        """.trimIndent()
        proseseringsMetrikker( "syfoinntektsmelding_inntektsmeldinger_kobling", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingNySak() {
        val metricHelpText = """
                  "kobling", $NY_SAK
        """.trimIndent()
        proseseringsMetrikker( "syfoinntektsmelding_inntektsmeldinger_kobling", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingSykepengerUtland() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_sykepenger_utland", metricHelpText).labels("info").inc()
    }

    fun tellFeiletBakgrunnsjobb() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_bakgrunnsjobb_feilet", metricHelpText).labels("info").inc()
    }

    fun tellStoppetBakgrunnsjobb() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_bakgrunnsjobb_stoppet", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingfeil() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_inntektsmeldingfeil", metricHelpText).labels("error").inc()
    }

    fun tellBehandlingsfeil(feiltype: Feiltype) {
        val metricHelpText = """
                "feiltype", ${feiltype.navn}
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_behandlingsfeil", metricHelpText).labels("error").inc()
    }

    fun tellIkkebehandlet() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_ikkebehandlet", metricHelpText).labels("info").inc()
    }

    fun tellJournalpoststatus(status: JournalStatus) {
        val metricHelpText = """
                "status", ${status.name}
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_journalpost", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingLagtPåTopic() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_inntektsmelding_lagt_pa_topic", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingUtenArkivReferanse() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_inntektsmelding_uten_arkivreferanse", metricHelpText).labels("info").inc()
    }

    fun tellInntektsmeldingerRedusertEllerIngenUtbetaling(begrunnelse: String?) {
        val metricHelpText = """
               "begrunnelse", $begrunnelse
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_redusert_eller_ingen_utbetaling", metricHelpText).labels("info").inc()
    }

    fun tellArbeidsgiverperioder(antall: String?) {
        val metricHelpText = """
              "antall", $antall
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_arbeidsgiverperioder", metricHelpText).labels("info").inc()
    }

    fun tellKreverRefusjon(beløp: Int) {
        val metricHelpText = ""
        if (beløp <= 0) return
        proseseringsMetrikker("syfoinntektsmelding_arbeidsgiver_krever_refusjon", metricHelpText).labels("info").inc()
        proseseringsMetrikker("syfoinntektsmelding_arbeidsgiver_krever_refusjon_beloep", metricHelpText).labels("info")
            .inc((beløp / 1000).toDouble()) // Teller beløpet i antall tusener for å unngå overflow
    }

    fun tellNaturalytelse() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_faar_naturalytelse", metricHelpText).labels("info").inc()
    }

    fun tellOpprettOppgave(eksisterer: Boolean) {
        val metricHelpText = """
            "eksisterer", ${if (eksisterer) "J" else "N"}
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_opprett_oppgave", metricHelpText).labels("info").inc()
    }

    fun tellOpprettFordelingsoppgave() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_opprett_fordelingsoppgave", metricHelpText).labels("info").inc()
    }

    fun tellLagreFeiletMislykkes() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_feilet_lagring_mislykkes", metricHelpText).labels("info").inc()
    }

    fun tellRekjørerFeilet() {
        val metricHelpText = ""
        proseseringsMetrikker("syfoinntektsmelding_rekjorer", metricHelpText).labels("info").inc()
    }

    companion object {
        private const val OVERLAPPENDE = "overlappende"
        private const val SAK_FRA_SYFO = "sakFraSyfo"
        private const val NY_SAK = "nySak"
    }
}
