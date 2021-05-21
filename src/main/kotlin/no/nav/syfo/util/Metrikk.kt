package no.nav.syfo.util

import io.prometheus.client.Counter
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding



class Metrikk {

    private val METRICS_NS = "spinn"

    val counters = HashMap<String, Counter>()

    private fun proseseringsMetrikker(metricName: String, metricHelpText: String, labelNames: Array<String> ): Counter {
        return if (counters.containsKey(metricName)) {
            counters[metricName]!!
        } else {
            val counter = Counter.build()
                .namespace(METRICS_NS)
                .name(metricName)
                .labelNames(*labelNames)
                .help(metricHelpText)
                .register()

            counters[metricName] = counter
            counter
        }
    }

    fun tellInntektsmeldingerMottatt(inntektsmelding: Inntektsmelding) {
        val labelNames = arrayOf("type", "info","harArbeidsforholdId",
            if (inntektsmelding.arbeidsforholdId != null) "J" else "N",
           "arsakTilSending",
            inntektsmelding.arsakTilInnsending)

        proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_mottatt",
            "Metrikker for mottatt inntektsmeldinger", labelNames).inc()
    }

    fun tellInntektsmeldingerJournalfort() = proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_journalfort",
            "Metrikker for journalfort inntektsmeldinger",arrayOf("type", "info")).inc()


    fun tellOverlappendeInntektsmelding() = proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_kobling",
            "Metrikker for inntektsmeldinger kobling",arrayOf("type", "info", "kobling", OVERLAPPENDE)).inc()

    fun tellInntektsmeldingSaksIdFraSyfo() = proseseringsMetrikker( "syfoinntektsmelding_inntektsmeldinger_kobling", "Metrikker for inntektsmeldinger kobling",
            arrayOf("type", "info",  "kobling", SAK_FRA_SYFO)).inc()


    fun tellInntektsmeldingNySak() =
        proseseringsMetrikker( "syfoinntektsmelding_inntektsmeldinger_kobling",
            "Metrikker for inntektsmeldinger kobling", arrayOf("type", "info",  "kobling", NY_SAK)).inc()


    fun tellInntektsmeldingSykepengerUtland() =
        proseseringsMetrikker("syfoinntektsmelding_sykepenger_utland", "Metrikker for sykepenger utland", arrayOf("")).inc()


    fun tellFeiletBakgrunnsjobb() = proseseringsMetrikker("syfoinntektsmelding_bakgrunnsjobb_feilet",
        "Metrikker for feilt bakgrunnjobb", arrayOf("")).inc()

    fun tellStoppetBakgrunnsjobb() = proseseringsMetrikker("syfoinntektsmelding_bakgrunnsjobb_stoppet",
        "Metrikker for stoppet bakgrunnsjob", arrayOf("")).inc()


    fun tellInntektsmeldingfeil() = proseseringsMetrikker("syfoinntektsmelding_inntektsmeldingfeil",
        "Metrikker for inntektsmelding feiler", arrayOf("type", "error")).inc()


    fun tellBehandlingsfeil(feiltype: Feiltype) = proseseringsMetrikker("syfoinntektsmelding_behandlingsfeil",
        "Metrikker for behandlet feiler", arrayOf("feiltype", feiltype.navn)).inc()


    fun tellIkkebehandlet() = proseseringsMetrikker("syfoinntektsmelding_ikkebehandlet",
            "Metrikker for ikke behandlet", arrayOf("")).inc()


    fun tellJournalpoststatus(status: JournalStatus) {
        val metricHelpText = """
                "status", ${status.name}
        """.trimIndent()
        proseseringsMetrikker("syfoinntektsmelding_journalpost",
            "Metrikk for journal post status", arrayOf("type", "info", "status", status.name)).inc()
    }

    fun tellInntektsmeldingLagtPåTopic() = proseseringsMetrikker("syfoinntektsmelding_inntektsmelding_lagt_pa_topic",
        "Metrikker for InntektsmeldingLagt På topic", arrayOf("")).inc()


    fun tellInntektsmeldingUtenArkivReferanse() = proseseringsMetrikker("syfoinntektsmelding_inntektsmelding_uten_arkivreferanse",
        "Metrikker for InntektsmeldingLagt uten arkiv refereanse", arrayOf("")).inc()


    fun tellInntektsmeldingerRedusertEllerIngenUtbetaling(begrunnelse: String?) {
        proseseringsMetrikker("syfoinntektsmelding_redusert_eller_ingen_utbetaling",
            "Metrikker for Inntektsmeldinger Redusert Eller Ingen Utbetaling", arrayOf("type", "info", "begrunnelse", begrunnelse!!)).inc()
    }

    fun tellArbeidsgiverperioder(antall: String?) {
        proseseringsMetrikker("syfoinntektsmelding_arbeidsgiverperioder",
            "Metrikker for arbeidsgiverperioder", arrayOf("antall", antall!!)).inc()
    }

    fun tellKreverRefusjon(beløp: Int) {
        val metricHelpText = "tellKreverRefusjon"
        if (beløp <= 0) return
        proseseringsMetrikker("syfoinntektsmelding_arbeidsgiver_krever_refusjon",
            "Metrikker for KreverRefusjon", arrayOf("")).inc()
        proseseringsMetrikker("syfoinntektsmelding_arbeidsgiver_krever_refusjon_beloep",
            "Metrikker for KreverRefusjon", arrayOf("")).inc((beløp / 1000).toDouble()) // Teller beløpet i antall tusener for å unngå overflow
    }

    fun tellNaturalytelse() = proseseringsMetrikker("syfoinntektsmelding_faar_naturalytelse",
            "Metrikker for Naturalytelse", arrayOf("")).inc()


    fun tellOpprettOppgave(eksisterer: Boolean) {
        proseseringsMetrikker("syfoinntektsmelding_opprett_oppgave",
            "Metrikker for OpprettOppgave", arrayOf("eksisterer", if (eksisterer) "J" else "N")).inc()
    }

    fun tellOpprettFordelingsoppgave() {
        proseseringsMetrikker("syfoinntektsmelding_opprett_fordelingsoppgave",
            "Metrikker for Opprett Fordelingsoppgave", arrayOf("")).inc()

    }

    fun tellLagreFeiletMislykkes() {
        proseseringsMetrikker("syfoinntektsmelding_feilet_lagring_mislykkes",
            "Metrikker for Lagre feilet mislykkes", arrayOf("")).inc()
    }

    fun tellRekjørerFeilet() {
        val metricHelpText = "tellRekjørerFeilet"
        proseseringsMetrikker("syfoinntektsmelding_rekjorer",
            "Metrikker for rekjører Feilet", arrayOf("")).inc()
    }

    companion object {
        private const val OVERLAPPENDE = "overlappende"
        private const val SAK_FRA_SYFO = "sakFraSyfo"
        private const val NY_SAK = "nySak"
    }
}
