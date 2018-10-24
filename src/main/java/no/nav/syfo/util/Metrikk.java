package no.nav.syfo.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

@Controller
public class Metrikk {

    private final MeterRegistry registry;

    @Inject
    public Metrikk(MeterRegistry registry) {
        this.registry = registry;
    }

    public void tellInntektsmeldingerMottat() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_mottatt", Tags.of("type", "info")).increment();
    }

    public void tellInntektsmeldingerJournalfort() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_journalfort", Tags.of("type", "info")).increment();
    }

    public void tellInntektsmeldingerMedArbeidsforholdId() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_med_arbeidsforhold_id", Tags.of("type", "info")).increment();
    }

    public void tellInntektsmeldingerSomErEndringsmeldinger() {
        registry.counter("syfoinntektsmelding_inntektsmeldinger_som_er_endringsmeldinger", Tags.of("type", "info")).increment();
    }
}
