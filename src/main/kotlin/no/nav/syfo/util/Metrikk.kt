package no.nav.syfo.util

import io.prometheus.client.Counter
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding



class Metrikk {

    private val METRICS_NS = "spinn"
    val counters = HashMap<String, Counter>()

    private fun proseseringsMetrikker(metricName: String, metricHelpText: String, vararg labelNames : String ): Counter {
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

    val NO_LABELS = ""
    val INNTEKTSMELDINGERMOTTATT = proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_mottatt",
        "Metrikker for mottatt inntektsmeldinger", "type","harArbeidsforholdId","arsakTilSending")

    val INNTEKTS_MELDINGER_JOURNALFORT = proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_journalfort",
    "Metrikker for journalfort inntektsmeldinger", "type")

    val OVERLAPPENDEINNTEKTSMELDING = proseseringsMetrikker("syfoinntektsmelding_inntektsmeldinger_kobling",
    "Metrikker for inntektsmeldinger kobling","type", "kobling")

    val INNTEKTSMELDINGSAKSIDFRASYFO = proseseringsMetrikker( "syfoinntektsmelding_inntektsmeldinger_kobling",
        "Metrikker for inntektsmeldinger kobling",  "type",  "kobling")

    val INNTEKTSMELDINGNYSAK = proseseringsMetrikker( "syfoinntektsmelding_inntektsmeldinger_kobling",
    "Metrikker for inntektsmeldinger kobling", "type",  "kobling")

    val INNTEKTSMELDINGSYKEPENGERUTLAND = proseseringsMetrikker("syfoinntektsmelding_sykepenger_utland",
        "Metrikker for sykepenger utland", NO_LABELS)

    val FEILETBAKGRUNNSJOBB = proseseringsMetrikker("syfoinntektsmelding_bakgrunnsjobb_feilet",
    "Metrikker for feilt bakgrunnjobb", NO_LABELS)

    val STOPPETBAKGRUNNSJOBB = proseseringsMetrikker("syfoinntektsmelding_bakgrunnsjobb_stoppet",
    "Metrikker for stoppet bakgrunnsjob", NO_LABELS)

    val INNTEKTSMELDINGFEIL = proseseringsMetrikker("syfoinntektsmelding_inntektsmeldingfeil",
    "Metrikker for inntektsmelding feiler", NO_LABELS)

    val BEHANDLINGSFEIL = proseseringsMetrikker("syfoinntektsmelding_behandlingsfeil",
    "Metrikker for behandlet feiler", NO_LABELS)

    val IKKEBEHANDLET = proseseringsMetrikker("syfoinntektsmelding_ikkebehandlet",
    "Metrikker for ikke behandlet", NO_LABELS)

    val JOURNALPOSTSTATUS = proseseringsMetrikker("syfoinntektsmelding_journalpost",
            "Metrikk for journal post status", "type", "status")

    val INNTEKTSMELDINGLAGTPÅTOPIC = proseseringsMetrikker("syfoinntektsmelding_inntektsmelding_lagt_pa_topic",
    "Metrikker for InntektsmeldingLagt På topic", NO_LABELS)

    val INNTEKTSMELDINGUTENARKIVREFERANSE = proseseringsMetrikker("syfoinntektsmelding_inntektsmelding_uten_arkivreferanse",
    "Metrikker for InntektsmeldingLagt uten arkiv refereanse", NO_LABELS)

    val INNTEKTSMELDINGERREDUSERTELLERINGENUTBETALING = proseseringsMetrikker("syfoinntektsmelding_redusert_eller_ingen_utbetaling",
            "Metrikker for Inntektsmeldinger Redusert Eller Ingen Utbetaling", "type", "begrunnelse")

    val ARBEIDSGIVERPERIODER = proseseringsMetrikker("syfoinntektsmelding_arbeidsgiverperioder",
            "Metrikker for arbeidsgiverperioder", "antall")

    val KREVERREFUSJON =  proseseringsMetrikker("syfoinntektsmelding_arbeidsgiver_krever_refusjon",
        "Metrikker for KreverRefusjon", NO_LABELS)

    val NATURALYTELSE = proseseringsMetrikker("syfoinntektsmelding_faar_naturalytelse",
    "Metrikker for Naturalytelse", NO_LABELS)

    val OPPRETTOPPGAVE = proseseringsMetrikker("syfoinntektsmelding_opprett_oppgave",
            "Metrikker for OpprettOppgave", "eksisterer")

    val OPPRETTFORDELINGSOPPGAVE = proseseringsMetrikker("syfoinntektsmelding_opprett_fordelingsoppgave",
            "Metrikker for Opprett Fordelingsoppgave", NO_LABELS)

    val LAGREFEILETMISLYKKES = proseseringsMetrikker("syfoinntektsmelding_feilet_lagring_mislykkes",
            "Metrikker for Lagre feilet mislykkes", NO_LABELS)

    val REKJØRERFEILET = proseseringsMetrikker("syfoinntektsmelding_rekjorer",
            "Metrikker for rekjører Feilet", NO_LABELS)



    fun tellInntektsmeldingerMottatt(inntektsmelding: Inntektsmelding) {
        val labelNames = arrayOf("info",
            if (inntektsmelding.arbeidsforholdId != null) "J" else "N",
            inntektsmelding.arsakTilInnsending)
        INNTEKTSMELDINGERMOTTATT.labels(*labelNames).inc()
    }

    fun tellInntektsmeldingerJournalfort() = INNTEKTS_MELDINGER_JOURNALFORT.labels("info").inc()

    fun tellOverlappendeInntektsmelding() = OVERLAPPENDEINNTEKTSMELDING.labels("info", OVERLAPPENDE).inc()

    fun tellInntektsmeldingSaksIdFraSyfo() = INNTEKTSMELDINGSAKSIDFRASYFO.labels("info", SAK_FRA_SYFO).inc()

    fun tellInntektsmeldingNySak() = INNTEKTSMELDINGNYSAK.labels("info", NY_SAK).inc()

    fun tellInntektsmeldingSykepengerUtland() = INNTEKTSMELDINGSYKEPENGERUTLAND.inc()

    fun tellFeiletBakgrunnsjobb() = FEILETBAKGRUNNSJOBB.inc()

    fun tellStoppetBakgrunnsjobb() = STOPPETBAKGRUNNSJOBB.inc()

    fun tellInntektsmeldingfeil() = INNTEKTSMELDINGFEIL.labels("error").inc()

    fun tellBehandlingsfeil(feiltype: Feiltype) = BEHANDLINGSFEIL.labels("${feiltype.navn}").inc()

    fun tellIkkebehandlet() = IKKEBEHANDLET.inc()

    fun tellJournalpoststatus(status: JournalStatus) = JOURNALPOSTSTATUS.labels("info", "${status.name}" ).inc()

    fun tellInntektsmeldingLagtPåTopic() = INNTEKTSMELDINGLAGTPÅTOPIC.inc()

    fun tellInntektsmeldingUtenArkivReferanse() = INNTEKTSMELDINGUTENARKIVREFERANSE.inc()

    fun tellInntektsmeldingerRedusertEllerIngenUtbetaling(begrunnelse: String?) = INNTEKTSMELDINGERREDUSERTELLERINGENUTBETALING.labels("info", "$begrunnelse").inc()

    fun tellArbeidsgiverperioder(antall: String?) = ARBEIDSGIVERPERIODER.labels("$antall").inc()

    fun tellKreverRefusjon(beløp: Int) {
        if (beløp <= 0) return
        KREVERREFUSJON.inc()
        KREVERREFUSJON.inc((beløp / 1000).toDouble()) // Teller beløpet i antall tusener for å unngå overflow
    }

    fun tellNaturalytelse() = NATURALYTELSE.inc()

    fun tellOpprettOppgave(eksisterer: Boolean) = OPPRETTOPPGAVE.labels(if (eksisterer) "J" else "N").inc()

    fun tellOpprettFordelingsoppgave() = OPPRETTFORDELINGSOPPGAVE.inc()

    fun tellLagreFeiletMislykkes() = LAGREFEILETMISLYKKES.inc()

    fun tellRekjørerFeilet() = REKJØRERFEILET.inc()


    companion object {
        private const val OVERLAPPENDE = "overlappende"
        private const val SAK_FRA_SYFO = "sakFraSyfo"
        private const val NY_SAK = "nySak"
    }
}
