package no.nav.syfo.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import no.nav.syfo.domain.Inntektsmelding;
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
        String arbeidsforholdId = inntektsmelding.getArbeidsforholdId() == null
                ? "null"
                : inntektsmelding.getArbeidsforholdId();
        registry.counter(
                "syfoinntektsmelding_inntektsmeldinger_mottatt",
                Tags.of(
                        "type", "info",
                        "arbeidsforholdId", arbeidsforholdId,
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

    public void tellInntektsmeldingfeil() {
        registry.counter("syfoinntektsmelding_inntektsmeldingfeil", Tags.of("type", "error")).increment();
    }
}
