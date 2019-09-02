package no.nav.syfo.producer

import junit.framework.Assert.assertEquals
import no.nav.syfo.mapping.mapInntektsmelding
import no.nav.syfo.util.Metrikk
import org.junit.Test
import org.mockito.Mockito
import testutil.grunnleggendeInntektsmelding

class InntektsmeldingProducerTest {


    val kontraktInntektsmelding = mapInntektsmelding(grunnleggendeInntektsmelding, "0000")
    val producer = InntektsmeldingProducer("test.topic",
            "localhost:9092",
            "user",
            "pass",
            Mockito.mock(Metrikk::class.java))

    @Test
    fun skal_serialisere_inntektsmelding() {
        val jsonString = producer.serialiseringInntektsmelding(kontraktInntektsmelding)
        assertEquals(jsonString, """
            {"inntektsmeldingId":"","arbeidstakerFnr":"12345678901","arbeidstakerAktorId":"0000","virksomhetsnummer":"1234","arbeidsgiverAktorId":"","arbeidsgivertype":"VIRKSOMHET","refusjon":{},"endringIRefusjoner":[],"opphoerAvNaturalytelser":[],"gjenopptakelseNaturalytelser":[],"arbeidsgiverperioder":[{"fom":"2019-01-01","tom":"2019-02-01"}],"status":"GYLDIG"}
        """.trimIndent())
    }


}