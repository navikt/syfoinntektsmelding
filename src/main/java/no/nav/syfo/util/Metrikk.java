package no.nav.syfo.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import no.nav.syfo.domain.Inntektsmelding;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

@Controller
public class Metrikk {

    private final MeterRegistry registry;

    @Inject
    public Metrikk(MeterRegistry registry) {
        this.registry = registry;
    }

    public void tellInntektsmeldingerMottat(Inntektsmelding inntektsmelding) {
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
}
