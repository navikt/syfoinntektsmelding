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
                mapArbeidsgivertype(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null, arbeidsgiverPrivat = "00")),
                Arbeidsgivertype.PRIVAT)
    }

    @Test(expected = IllegalStateException::class)
    fun skal_gi_feilmelding_hvis_både_privat_og_virksomhet() {
        mapArbeidsgivertype(grunnleggendeInntektsmelding.copy(arbeidsgiverPrivat = "0"))
    }

    @Test(expected = IllegalStateException::class)
    fun skal_gi_feilmelding_hvis_hverken_privat_eller_virksomhet() {
        mapArbeidsgivertype(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null))
    }

}