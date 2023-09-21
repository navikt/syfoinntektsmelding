package no.nav.syfo.mapping

import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.BegrunnelseIngenEllerRedusertUtbetalingKode
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.grunnleggendeInntektsmelding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class InntektsmeldingMapperFraInternSyfoTilHAGKontraktTest {

    @Test
    fun mapInntektsmeldingKontraktUtenBegrunnelseOgBeløp() {
        val syfoInternInntektsmelding = grunnleggendeInntektsmelding
        val inntektsmelding = mapInntektsmeldingKontrakt(
            syfoInternInntektsmelding,
            "123",
            Gyldighetsstatus.GYLDIG,
            "arkivref-123",
            UUID.randomUUID().toString()
        )
        assertNull(inntektsmelding.bruttoUtbetalt)
        assertEquals("", inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt)
    }
    @Test
    fun mapInntektsmeldingKontrakt() {
        val bruttoUtbetalt = BigDecimal(39013)
        val begrunnelse = BegrunnelseIngenEllerRedusertUtbetalingKode.FERIE_ELLER_AVSPASERING.value
        val innsenderNavn = "André Bjørke"
        val innsenderTelefon = "22555555"
        val syfoInternInntektsmelding = grunnleggendeInntektsmelding.copy(
            bruttoUtbetalt = bruttoUtbetalt,
            begrunnelseRedusert = begrunnelse,
            kontaktinformasjon = Kontaktinformasjon(navn = innsenderNavn, telefon = innsenderTelefon)
        )
        val inntektsmelding = mapInntektsmeldingKontrakt(
            syfoInternInntektsmelding,
            "123",
            Gyldighetsstatus.GYLDIG,
            "arkivref-123",
            UUID.randomUUID().toString()
        )
        assertEquals(bruttoUtbetalt, inntektsmelding.bruttoUtbetalt)
        assertEquals(begrunnelse, inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt)
        assertEquals(innsenderNavn, inntektsmelding.innsenderFulltNavn)
        assertEquals(innsenderTelefon, inntektsmelding.innsenderTelefon)
    }
}
