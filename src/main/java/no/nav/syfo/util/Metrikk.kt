package no.nav.syfo.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import no.nav.syfo.behandling.Feiltype;
import no.nav.syfo.domain.JournalStatus;
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

@Controller
public class Metrikk {

    private static final String OVERLAPPENDE = "overlappende";
    private static final String SAK_FRA_SYFO = "sakFraSyfo";
    private static final String NY_SAK = "nySak";

    private final MeterRegistry registry;

    @Inject
    public Metrikk(MeterRegistry registry) {
        this.registry = registry;
    }

    public void tellInntektsmeldingerMottatt(Inntektsmelding inntektsmelding) {
        boolean harArbeidsforholdId = inntektsmelding.getArbeidsforholdId() != null;
        registry.counter(
            "syfoinntektsmelding_inntektsmeldinger_mottatt",
            Tags.of(
                "type", "info",
                "harArbeidsforholdId", harArbeidsforholdId ? "J" : "N",
                "arsakTilSending", inntektsmelding.getArsakTilInnsending()
            ))
            .increment();
    }

    public void tellInntektsmeldingerJournalfort() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_journalfort", Tags.of("type", "info")).increment();
    }

    public void tellOverlappendeInntektsmelding() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_kobling", Tags.of(
            "type", "info",
            "kobling", OVERLAPPENDE
        )).increment();
    }

    public void tellInntektsmeldingSaksIdFraSyfo() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_kobling", Tags.of(
            "type", "info",
            "kobling", SAK_FRA_SYFO
        )).increment();
    }

    public void tellInntektsmeldingNySak() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_kobling", Tags.of(
            "type", "info",
            "kobling", NY_SAK
        )).increment();
    }

    public void tellInntektsmeldingSykepengerUtland() {
        registry.counter("syfoinntektsmelding_sykepenger_utland").increment();
    }

    public void tellFeiletBakgrunnsjobb() {
        registry.counter("syfoinntektsmelding_bakgrunnsjobb_feilet").increment();
    }

    public void tellStoppetBakgrunnsjobb() {
        registry.counter("syfoinntektsmelding_bakgrunnsjobb_stoppet").increment();
    }

    public void tellInntektsmeldingfeil() {
        registry.counter("syfoinntektsmelding_inntektsmeldingfeil", Tags.of("type", "error")).increment();
    }

    public void tellBehandlingsfeil(Feiltype feiltype) {
        registry.counter("syfoinntektsmelding_behandlingsfeil", Tags.of("feiltype", feiltype.getNavn())).increment();
    }

    public void tellIkkebehandlet() {
        registry.counter("syfoinntektsmelding_ikkebehandlet").increment();
    }

    public void tellJournalpoststatus(JournalStatus status) {
        registry
            .counter("syfoinntektsmelding_journalpost", Tags.of("type", "info", "status", status.name()))
            .increment();
    }

    public void tellInntektsmeldingLagtPåTopic() {
        registry
            .counter("syfoinntektsmelding_inntektsmelding_lagt_pa_topic").increment();
    }

    public void tellInntektsmeldingUtenArkivReferanse() {
        registry
            .counter("syfoinntektsmelding_inntektsmelding_uten_arkivreferanse").increment();
    }

    public void tellInntektsmeldingerRedusertEllerIngenUtbetaling(String begrunnelse) {
        registry.counter("syfoinntektsmelding_redusert_eller_ingen_utbetaling", Tags.of("type", "info", "begrunnelse", begrunnelse)).increment();
    }

    public void tellArbeidsgiverperioder(String antall) {
        registry
            .counter("syfoinntektsmelding_arbeidsgiverperioder", Tags.of("antall", antall)).increment();
    }

    public void tellKreverRefusjon(int beløp) {
        if (beløp <= 0)
            return;
        registry
            .counter("syfoinntektsmelding_arbeidsgiver_krever_refusjon").increment();
        registry
            .counter("syfoinntektsmelding_arbeidsgiver_krever_refusjon_beloep").increment(beløp / 1000); // Teller beløpet i antall tusener for å unngå overflow
    }

    public void tellNaturalytelse() {
        registry
            .counter("syfoinntektsmelding_faar_naturalytelse").increment();
    }

    public void tellOpprettOppgave(boolean eksisterer) {
        registry
            .counter("syfoinntektsmelding_opprett_oppgave", Tags.of("eksisterer", eksisterer ? "J" : "N")).increment();
    }

    public void tellOpprettFordelingsoppgave() {
        registry
            .counter("syfoinntektsmelding_opprett_fordelingsoppgave").increment();
    }

    public void tellLagreFeiletMislykkes() {
        registry
            .counter("syfoinntektsmelding_feilet_lagring_mislykkes").increment();
    }

    public void tellRekjørerFeilet() {
        registry
            .counter("syfoinntektsmelding_rekjorer").increment();
    }

}
