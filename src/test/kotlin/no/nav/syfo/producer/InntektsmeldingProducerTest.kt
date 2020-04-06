package no.nav.syfo.producer

import junit.framework.Assert.assertEquals
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.util.Metrikk
import org.junit.Test
import org.mockito.Mockito
import testutil.grunnleggendeInntektsmelding

class InntektsmeldingProducerTest {


    val kontraktInntektsmelding = mapInntektsmeldingKontrakt(grunnleggendeInntektsmelding, "0000", Gyldighetsstatus.GYLDIG, arkivreferanse = "AR123", uuid = "ID")
    val producer = InntektsmeldingProducer(
            "localhost:9092",
            "user",
            "pass",
            Mockito.mock(Metrikk::class.java))

    @Test
    fun skal_serialisere_og_deserialisere_inntektsmelding() {
        val jsonString = producer.serialiseringInntektsmelding(kontraktInntektsmelding)
        assertEquals("""
            {"inntektsmeldingId":"ID","arbeidstakerFnr":"12345678901","arbeidstakerAktorId":"0000","virksomhetsnummer":"1234","begrunnelseForReduksjonEllerIkkeUtbetalt":"","arbeidsgivertype":"VIRKSOMHET","refusjon":{},"endringIRefusjoner":[],"opphoerAvNaturalytelser":[],"gjenopptakelseNaturalytelser":[],"arbeidsgiverperioder":[{"fom":"2019-01-01","tom":"2019-02-01"}],"status":"GYLDIG","arkivreferanse":"AR123","ferieperioder":[],"foersteFravaersdag":"2019-10-05","mottattDato":"2019-10-25T00:00:00"}
        """.trimIndent(),
                jsonString)
        val deserialisertInntektsmelding = producer.objectMapper.readValue(jsonString, Inntektsmelding::class.java)
        assertEquals(kontraktInntektsmelding, deserialisertInntektsmelding)
    }


}
