package no.nav.syfo.syfoinnteksmelding.util

import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.util.validerInntektsmelding
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import testutil.grunnleggendeInntektsmelding

class InntektsmeldingValidatorKtTest {

    @Test
    fun skal_validere_normal_inntektsmelding_ok() {
        assertEquals(validerInntektsmelding(grunnleggendeInntektsmelding), Gyldighetsstatus.GYLDIG)
    }

    @Test
    fun skal_ikke_validere_ok_hvis_både_privat_og_virksomhet() {
        assertEquals(validerInntektsmelding(grunnleggendeInntektsmelding.copy(arbeidsgiverPrivatFnr = "0")), Gyldighetsstatus.MANGELFULL)
    }

    @Test
    fun skal_ikke_validere_ok__hvis_hverken_privat_eller_virksomhet() {
        assertEquals(validerInntektsmelding(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null)), Gyldighetsstatus.MANGELFULL)
    }

}
