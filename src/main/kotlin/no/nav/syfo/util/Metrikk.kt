package no.nav.syfo.util

import io.prometheus.client.Counter
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding

class Metrikk {
    private val metricsNs = "spinn"
    val counters = HashMap<String, Counter>()

    private fun proseseringsMetrikker(
        metricName: String,
        metricHelpText: String,
        vararg labelNames: String,
    ): Counter =
        if (counters.containsKey(metricName)) {
            counters[metricName]!!
        } else {
            val counter =
                Counter
                    .build()
                    .namespace(metricsNs)
                    .name(metricName)
                    .labelNames(*labelNames)
                    .help(metricHelpText)
                    .register()

            counters[metricName] = counter
            counter
        }

    val inntektsmeldingerMottatt =
        proseseringsMetrikker(
            "syfoinntektsmelding_inntektsmeldinger_mottatt",
            "Metrikker for mottatt inntektsmeldinger",
            "type",
            "harArbeidsforholdId",
            "arsakTilSending",
        )

    val inntektsmeldingerJournalfort =
        proseseringsMetrikker(
            "syfoinntektsmelding_inntektsmeldinger_journalfort",
            "Metrikker for journalfort inntektsmeldinger",
            "type",
        )

    val inntektsmeldingerFeilregistrert =
        proseseringsMetrikker(
            "syfoinntektsmelding_inntektsmeldinger_feilregistrert",
            "Metrikker for feilregistrerte inntektsmeldinger",
            "type",
        )

    val inntektsmeldingSykepengerUtland =
        proseseringsMetrikker(
            "syfoinntektsmelding_sykepenger_utland",
            "Metrikker for sykepenger utland",
            "type",
        )

    val behandlingsfeil =
        proseseringsMetrikker(
            "syfoinntektsmelding_behandlingsfeil",
            "Metrikker for behandlet feiler",
            "type",
        )

    val journalpoststatus =
        proseseringsMetrikker(
            "syfoinntektsmelding_journalpost",
            "Metrikk for journal post status",
            "type",
            "status",
        )

    val inntektsmeldingUtenArkivreferanse =
        proseseringsMetrikker(
            "syfoinntektsmelding_inntektsmelding_uten_arkivreferanse",
            "Metrikker for InntektsmeldingLagt uten arkiv refereanse",
            "type",
        )

    val inntektsmeldingerRedusertEllerIngenUtbetaling =
        proseseringsMetrikker(
            "syfoinntektsmelding_redusert_eller_ingen_utbetaling",
            "Metrikker for Inntektsmeldinger Redusert Eller Ingen Utbetaling",
            "type",
            "begrunnelse",
        )

    val arbeidsgiverperioder =
        proseseringsMetrikker(
            "syfoinntektsmelding_arbeidsgiverperioder",
            "Metrikker for arbeidsgiverperioder",
            "type",
            "antall",
        )

    val kreverRefusjon =
        proseseringsMetrikker(
            "syfoinntektsmelding_arbeidsgiver_krever_refusjon",
            "Metrikker for KreverRefusjon",
            "type",
        )

    val funksjonellLikhet =
        proseseringsMetrikker(
            "syfoinntektsmelding_likhet",
            "Metrikker for funksjonelt like",
            "type",
        )

    val naturalytelse =
        proseseringsMetrikker(
            "syfoinntektsmelding_faar_naturalytelse",
            "Metrikker for Naturalytelse",
            "type",
        )

    val opprettOppgave =
        proseseringsMetrikker(
            "syfoinntektsmelding_opprett_oppgave",
            "Metrikker for OpprettOppgave",
            "type",
            "eksisterer",
        )

    val opprettFordelingsoppgave =
        proseseringsMetrikker(
            "syfoinntektsmelding_opprett_fordelingsoppgave",
            "Metrikker for Opprett Fordelingsoppgave",
            "type",
        )

    val rekjørerFeilet =
        proseseringsMetrikker(
            "syfoinntektsmelding_rekjorer",
            "Metrikker for rekjører Feilet",
            "type",
        )

    val utsattOppgaveUkjent =
        proseseringsMetrikker(
            "syfoinntektsmelding_utsatt_oppgave_ukjent",
            "Mottok oppdatering på en ukjent oppgave",
            "type",
        )
    val utsattOppgaveUtsett =
        proseseringsMetrikker(
            "syfoinntektsmelding_utsatt_oppgave_utsett",
            "Oppdaterte timeout for inntektsmelding",
            "type",
        )
    val utsattOppgaveForkast =
        proseseringsMetrikker(
            "syfoinntektsmelding_utsatt_oppgave_forkast",
            "Forkaster oppgave",
            "type",
        )
    val utsattOppgaveOpprett =
        proseseringsMetrikker(
            "syfoinntektsmelding_utsatt_oppgave_opprett",
            "Oppretter oppgave",
            "type",
        )

    val utsattOppgaveOpprettTimeout =
        proseseringsMetrikker(
            "syfoinntektsmelding_utsatt_oppgave_opprett_timeout",
            "Oppretter oppgave pga timeout",
            "type",
        )
    val utsattOppgaveIrrelevant =
        proseseringsMetrikker(
            "syfoinntektsmelding_utsatt_oppgave_irrelevant",
            "Irrelevant dokumentid",
            "type",
        )

    fun tellInntektsmeldingerMottatt(inntektsmelding: Inntektsmelding) {
        val labelNames =
            arrayOf(
                "info",
                if (inntektsmelding.arbeidsforholdId != null) "J" else "N",
                inntektsmelding.arsakTilInnsending,
            )
        inntektsmeldingerMottatt.labels(*labelNames).inc()
    }

    fun tellInntektsmeldingerJournalfort() = inntektsmeldingerJournalfort.labels("info").inc()

    fun tellInntektsmeldingerFeilregistrert() = inntektsmeldingerFeilregistrert.labels("info").inc()

    fun tellInntektsmeldingSykepengerUtland() = inntektsmeldingSykepengerUtland.labels("info").inc()

    fun tellBehandlingsfeil(feiltype: Feiltype) = behandlingsfeil.labels(feiltype.navn).inc()

    fun tellJournalpoststatus(status: JournalStatus) = journalpoststatus.labels("info", status.name).inc()

    fun tellInntektsmeldingUtenArkivReferanse() = inntektsmeldingUtenArkivreferanse.labels("info").inc()

    fun tellInntektsmeldingerRedusertEllerIngenUtbetaling(begrunnelse: String?) =
        inntektsmeldingerRedusertEllerIngenUtbetaling
            .labels(
                "info",
                "$begrunnelse",
            ).inc()

    fun tellArbeidsgiverperioder(antall: String?) = arbeidsgiverperioder.labels("info", "$antall").inc()

    fun tellKreverRefusjon(beløp: Int) {
        if (beløp <= 0) return
        kreverRefusjon.labels("info").inc()
        kreverRefusjon.labels("info").inc((beløp / 1000).toDouble()) // Teller beløpet i antall tusener for å unngå overflow
    }

    fun tellNaturalytelse() = naturalytelse.labels("info").inc()

    fun tellFunksjonellLikhet() = funksjonellLikhet.labels("info").inc()

    fun tellOpprettOppgave(eksisterer: Boolean) = opprettOppgave.labels("info", if (eksisterer) "J" else "N").inc()

    fun tellOpprettFordelingsoppgave() = opprettFordelingsoppgave.labels("info").inc()

    fun tellRekjørerFeilet() = rekjørerFeilet.labels("warning").inc()

    fun tellUtsattOppgaveUkjent() = utsattOppgaveUkjent.labels("info").inc()

    fun tellUtsattOppgaveUtsett() = utsattOppgaveUtsett.labels("info").inc()

    fun tellUtsattOppgaveForkast() = utsattOppgaveForkast.labels("info").inc()

    fun tellUtsattOppgaveOpprett() = utsattOppgaveOpprett.labels("info").inc()

    fun tellUtsattOppgaveOpprettTimeout() = utsattOppgaveOpprettTimeout.labels("info").inc()

    fun tellUtsattOppgaveIrrelevant() = utsattOppgaveIrrelevant.labels("info").inc()
}
