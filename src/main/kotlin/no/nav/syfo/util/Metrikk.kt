package no.nav.syfo.util

import io.prometheus.client.Counter
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding

class Metrikk {

    private val METRICS_NS = "spinn"
    val counters = HashMap<String, Counter>()

    private fun proseseringsMetrikker(metricName: String, metricHelpText: String, vararg labelNames: String): Counter {
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

    val INNTEKTSMELDINGERMOTTATT = proseseringsMetrikker(
        "syfoinntektsmelding_inntektsmeldinger_mottatt",
        "Metrikker for mottatt inntektsmeldinger", "type", "harArbeidsforholdId", "arsakTilSending"
    )

    val INNTEKTS_MELDINGER_JOURNALFORT = proseseringsMetrikker(
        "syfoinntektsmelding_inntektsmeldinger_journalfort",
        "Metrikker for journalfort inntektsmeldinger", "type"
    )

    val OVERLAPPENDEINNTEKTSMELDING = proseseringsMetrikker(
        "syfoinntektsmelding_inntektsmeldinger_kobling",
        "Metrikker for inntektsmeldinger kobling", "type"
    )

    val SAK = proseseringsMetrikker(
        "syfoinntektsmelding_sak",
        "Hvordan man finner sakId", "type", "grunn"
    )

    val INNTEKTSMELDINGSYKEPENGERUTLAND = proseseringsMetrikker(
        "syfoinntektsmelding_sykepenger_utland",
        "Metrikker for sykepenger utland", "type"
    )

    val FEILETBAKGRUNNSJOBB = proseseringsMetrikker(
        "syfoinntektsmelding_bakgrunnsjobb_feilet",
        "Metrikker for feilt bakgrunnjobb", "type"
    )

    val STOPPETBAKGRUNNSJOBB = proseseringsMetrikker(
        "syfoinntektsmelding_bakgrunnsjobb_stoppet",
        "Metrikker for stoppet bakgrunnsjob", "type"
    )

    val BEHANDLINGSFEIL = proseseringsMetrikker(
        "syfoinntektsmelding_behandlingsfeil",
        "Metrikker for behandlet feiler", "type"
    )

    val JOURNALPOSTSTATUS = proseseringsMetrikker(
        "syfoinntektsmelding_journalpost",
        "Metrikk for journal post status", "type", "status"
    )

    val INNTEKTSMELDINGUTENARKIVREFERANSE = proseseringsMetrikker(
        "syfoinntektsmelding_inntektsmelding_uten_arkivreferanse",
        "Metrikker for InntektsmeldingLagt uten arkiv refereanse", "type"
    )

    val INNTEKTSMELDINGERREDUSERTELLERINGENUTBETALING = proseseringsMetrikker(
        "syfoinntektsmelding_redusert_eller_ingen_utbetaling",
        "Metrikker for Inntektsmeldinger Redusert Eller Ingen Utbetaling", "type", "begrunnelse"
    )

    val ARBEIDSGIVERPERIODER = proseseringsMetrikker(
        "syfoinntektsmelding_arbeidsgiverperioder",
        "Metrikker for arbeidsgiverperioder", "type", "antall"
    )

    val KREVERREFUSJON = proseseringsMetrikker(
        "syfoinntektsmelding_arbeidsgiver_krever_refusjon",
        "Metrikker for KreverRefusjon", "type"
    )

    val NATURALYTELSE = proseseringsMetrikker(
        "syfoinntektsmelding_faar_naturalytelse",
        "Metrikker for Naturalytelse", "type"
    )

    val OPPRETTOPPGAVE = proseseringsMetrikker(
        "syfoinntektsmelding_opprett_oppgave",
        "Metrikker for OpprettOppgave", "type", "eksisterer"
    )

    val OPPRETTFORDELINGSOPPGAVE = proseseringsMetrikker(
        "syfoinntektsmelding_opprett_fordelingsoppgave",
        "Metrikker for Opprett Fordelingsoppgave", "type"
    )

    val LAGREFEILETMISLYKKES = proseseringsMetrikker(
        "syfoinntektsmelding_feilet_lagring_mislykkes",
        "Metrikker for Lagre feilet mislykkes", "type"
    )

    val REKJØRERFEILET = proseseringsMetrikker(
        "syfoinntektsmelding_rekjorer",
        "Metrikker for rekjører Feilet", "type"
    )

    val UTSATT_OPPGAVE_UKJENT = proseseringsMetrikker(
        "syfoinntektsmelding_utsatt_oppgave_ukjent",
        "Mottok oppdatering på en ukjent oppgave", "type"
    )
    val UTSATT_OPPGAVE_UTSETT = proseseringsMetrikker(
        "syfoinntektsmelding_utsatt_oppgave_utsett",
        "Oppdaterte timeout for inntektsmelding", "type"
    )
    val UTSATT_OPPGAVE_UTSETT_UTEN_DATO = proseseringsMetrikker(
        "syfoinntektsmelding_utsatt_oppgave_ingen_dato",
        "Timeout på utsettelse mangler", "type"
    )
    val UTSATT_OPPGAVE_FORKAST = proseseringsMetrikker(
        "syfoinntektsmelding_utsatt_oppgave_forkast",
        "Forkaster oppgave", "type"
    )
    val UTSATT_OPPGAVE_OPPRETT = proseseringsMetrikker(
        "syfoinntektsmelding_utsatt_oppgave_opprett",
        "Oppretter oppgave", "type"
    )

    val UTSATT_OPPGAVE_OPPRETT_TIMEOUT = proseseringsMetrikker(
        "syfoinntektsmelding_utsatt_oppgave_opprett_timeout",
        "Oppretter oppgave pga timeout", "type"
    )
    val UTSATT_OPPGAVE_IRRELEVANT = proseseringsMetrikker(
        "syfoinntektsmelding_utsatt_oppgave_irrelevant",
        "Irrelevant dokumentid", "type"
    )

    fun tellInntektsmeldingerMottatt(inntektsmelding: Inntektsmelding) {
        val labelNames = arrayOf(
            "info",
            if (inntektsmelding.arbeidsforholdId != null) "J" else "N",
            inntektsmelding.arsakTilInnsending
        )
        INNTEKTSMELDINGERMOTTATT.labels(*labelNames).inc()
    }

    fun tellInntektsmeldingerJournalfort() = INNTEKTS_MELDINGER_JOURNALFORT.labels("info").inc()

    fun tellOverlappendeInntektsmelding() = OVERLAPPENDEINNTEKTSMELDING.labels("info", OVERLAPPENDE).inc()

    fun tellInntektsmeldingSaksIdFraSyfo() = SAK.labels("info", HENT).inc()
    fun tellInntektsmeldingSaksIdFraDB() = SAK.labels("info", EKSISTERER).inc()
    fun tellInntektsmeldingNySak() = SAK.labels("info", OPPRETT).inc()

    fun tellInntektsmeldingSykepengerUtland() = INNTEKTSMELDINGSYKEPENGERUTLAND.labels("info").inc()

    fun tellFeiletBakgrunnsjobb() = FEILETBAKGRUNNSJOBB.labels("info").inc()

    fun tellStoppetBakgrunnsjobb() = STOPPETBAKGRUNNSJOBB.labels("info").inc()

    fun tellBehandlingsfeil(feiltype: Feiltype) = BEHANDLINGSFEIL.labels("${feiltype.navn}").inc()

    fun tellJournalpoststatus(status: JournalStatus) = JOURNALPOSTSTATUS.labels("info", "${status.name}").inc()

    fun tellInntektsmeldingUtenArkivReferanse() = INNTEKTSMELDINGUTENARKIVREFERANSE.labels("info").inc()

    fun tellInntektsmeldingerRedusertEllerIngenUtbetaling(begrunnelse: String?) = INNTEKTSMELDINGERREDUSERTELLERINGENUTBETALING.labels("info", "$begrunnelse").inc()

    fun tellArbeidsgiverperioder(antall: String?) = ARBEIDSGIVERPERIODER.labels("info", "$antall").inc()

    fun tellKreverRefusjon(beløp: Int) {
        if (beløp <= 0) return
        KREVERREFUSJON.labels("info").inc()
        KREVERREFUSJON.labels("info").inc((beløp / 1000).toDouble()) // Teller beløpet i antall tusener for å unngå overflow
    }

    fun tellNaturalytelse() = NATURALYTELSE.labels("info").inc()

    fun tellOpprettOppgave(eksisterer: Boolean) = OPPRETTOPPGAVE.labels("info", if (eksisterer) "J" else "N").inc()

    fun tellOpprettFordelingsoppgave() = OPPRETTFORDELINGSOPPGAVE.labels("info").inc()

    fun tellLagreFeiletMislykkes() = LAGREFEILETMISLYKKES.labels("warning").inc()

    fun tellRekjørerFeilet() = REKJØRERFEILET.labels("warning").inc()

    fun tellUtsattOppgave_Ukjent() = UTSATT_OPPGAVE_UKJENT.labels("info").inc()
    fun tellUtsattOppgave_Utsett() = UTSATT_OPPGAVE_UTSETT.labels("info").inc()
    fun tellUtsattOppgave_UtenDato() = UTSATT_OPPGAVE_UTSETT_UTEN_DATO.labels("info").inc()
    fun tellUtsattOppgave_Forkast() = UTSATT_OPPGAVE_FORKAST.labels("info").inc()
    fun tellUtsattOppgave_Opprett() = UTSATT_OPPGAVE_OPPRETT.labels("info").inc()
    fun tellUtsattOppgave_OpprettTimeout() = UTSATT_OPPGAVE_OPPRETT_TIMEOUT.labels("info").inc()
    fun tellUtsattOppgave_Irrelevant() = UTSATT_OPPGAVE_IRRELEVANT.labels("info").inc()

    companion object {
        private const val OVERLAPPENDE = "overlappende"
        private const val HENT = "hent"
        private const val OPPRETT = "opprett"
        private const val EKSISTERER = "eksisterer"
    }
}
