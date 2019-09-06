package no.nav.syfo.mapping

import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import org.junit.Assert.assertEquals
import org.junit.Test
import testutil.FØRSTE_FEBRUAR
import testutil.FØRSTE_JANUAR
import testutil.grunnleggendeInntektsmelding

class InntektsmeldingMapperKtTest {


    @Test
    fun skal_mappe_enkel_periode() {
        val mappedePerioder = mapArbeidsgiverperioder(grunnleggendeInntektsmelding)
        assertEquals(mappedePerioder.size, 1)
        assertEquals(mappedePerioder[0].fom, FØRSTE_JANUAR)
        assertEquals(mappedePerioder[0].tom, FØRSTE_FEBRUAR)
    }


    @Test
    fun skal_finne_arbeidsgivertype_virksomhet() {
        assertEquals(mapArbeidsgivertype(grunnleggendeInntektsmelding), Arbeidsgivertype.VIRKSOMHET)
    }


    @Test
    fun skal_finne_arbeidsgivertype_privat() {
        assertEquals(
                mapArbeidsgivertype(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null, arbeidsgiverPrivatFnr = "00")),
                Arbeidsgivertype.PRIVAT)
    }

}