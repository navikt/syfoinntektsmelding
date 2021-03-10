package no.nav.syfo.util

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import org.springframework.stereotype.Controller
import javax.inject.Inject

@Controller
class Metrikk @Inject constructor(private val registry: MeterRegistry) {
    fun tellInntektsmeldingerMottatt(inntektsmelding: Inntektsmelding) {
        val harArbeidsforholdId = inntektsmelding.arbeidsforholdId != null
        registry.counter(
            "syfoinntektsmelding_inntektsmeldinger_mottatt",
            Tags.of(
                "type", "info",
                "harArbeidsforholdId", if (harArbeidsforholdId) "J" else "N",
                "arsakTilSending", inntektsmelding.arsakTilInnsending
            ))
            .increment()
    }

    fun tellInntektsmeldingerJournalfort() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_journalfort", Tags.of("type", "info")).increment()
    }

    fun tellOverlappendeInntektsmelding() {
        registry.counter(
            "syfoinntektsmelding_inntektsmeldinger_kobling", Tags.of(
                "type", "info",
                "kobling", OVERLAPPENDE
            )).increment()
    }

    fun tellInntektsmeldingSaksIdFraSyfo() {
        registry.counter(
            "syfoinntektsmelding_inntektsmeldinger_kobling", Tags.of(
                "type", "info",
                "kobling", SAK_FRA_SYFO
            )).increment()
    }

    fun tellInntektsmeldingNySak() {
        registry.counter(
            "syfoinntektsmelding_inntektsmeldinger_kobling", Tags.of(
                "type", "info",
                "kobling", NY_SAK
            )
        ).increment()
    }

    fun tellInntektsmeldingSykepengerUtland() {
        registry.counter("syfoinntektsmelding_sykepenger_utland").increment()
    }

    fun tellFeiletBakgrunnsjobb() {
        registry.counter("syfoinntektsmelding_bakgrunnsjobb_feilet").increment()
    }

    fun tellStoppetBakgrunnsjobb() {
        registry.counter("syfoinntektsmelding_bakgrunnsjobb_stoppet").increment()
    }

    fun tellInntektsmeldingfeil() {
        registry.counter("syfoinntektsmelding_inntektsmeldingfeil", Tags.of("type", "error")).increment()
    }

    fun tellBehandlingsfeil(feiltype: Feiltype) {
        registry.counter("syfoinntektsmelding_behandlingsfeil", Tags.of("feiltype", feiltype.navn)).increment()
    }

    fun tellIkkebehandlet() {
        registry.counter("syfoinntektsmelding_ikkebehandlet").increment()
    }

    fun tellJournalpoststatus(status: JournalStatus) {
        registry.counter("syfoinntektsmelding_journalpost", Tags.of("type", "info", "status", status.name))
            .increment()
    }

    fun tellInntektsmeldingLagtPåTopic() {
        registry.counter("syfoinntektsmelding_inntektsmelding_lagt_pa_topic").increment()
    }

    fun tellInntektsmeldingUtenArkivReferanse() {
        registry.counter("syfoinntektsmelding_inntektsmelding_uten_arkivreferanse").increment()
    }

    fun tellInntektsmeldingerRedusertEllerIngenUtbetaling(begrunnelse: String?) {
        registry.counter(
            "syfoinntektsmelding_redusert_eller_ingen_utbetaling",
            Tags.of("type", "info", "begrunnelse", begrunnelse)
        ).increment()
    }

    fun tellArbeidsgiverperioder(antall: String?) {
        registry.counter("syfoinntektsmelding_arbeidsgiverperioder", Tags.of("antall", antall)).increment()
    }

    fun tellKreverRefusjon(beløp: Int) {
        if (beløp <= 0) return
        registry.counter("syfoinntektsmelding_arbeidsgiver_krever_refusjon").increment()
        registry.counter("syfoinntektsmelding_arbeidsgiver_krever_refusjon_beloep")
            .increment((beløp / 1000).toDouble()) // Teller beløpet i antall tusener for å unngå overflow
    }

    fun tellNaturalytelse() {
        registry.counter("syfoinntektsmelding_faar_naturalytelse").increment()
    }

    fun tellOpprettOppgave(eksisterer: Boolean) {
        registry.counter("syfoinntektsmelding_opprett_oppgave", Tags.of("eksisterer", if (eksisterer) "J" else "N"))
            .increment()
    }

    fun tellOpprettFordelingsoppgave() {
        registry.counter("syfoinntektsmelding_opprett_fordelingsoppgave").increment()
    }

    fun tellLagreFeiletMislykkes() {
        registry.counter("syfoinntektsmelding_feilet_lagring_mislykkes").increment()
    }

    fun tellRekjørerFeilet() {
        registry.counter("syfoinntektsmelding_rekjorer").increment()
    }

    companion object {
        private const val OVERLAPPENDE = "overlappende"
        private const val SAK_FRA_SYFO = "sakFraSyfo"
        private const val NY_SAK = "nySak"
    }
}
