package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Bonus
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Feilregistrert
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Ferie
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Ferietrekk
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.InntektEndringAarsak
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.NyStilling
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.NyStillingsprosent
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Nyansatt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Permisjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Permittering
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Sykefravaer
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Tariffendring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.VarigLonnsendring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.syfo.domain.inntektsmelding.SpinnInntektEndringAarsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class MapInntektEndringAarsakFraSimbaTest {

    companion object {

        // SpinnInntektEndringAarsak som tilsvarer InntektEndringAarsak
        @JvmStatic
        fun inntektEndringerProvider() =
            listOf(
                Pair(Mock.spinnInntektEndringAarsak, Bonus()),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Feilregistrert"), Feilregistrert),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Ferie", perioder = listOf(Mock.spinnPeriode)), Ferie(liste = listOf(Mock.periode))),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Ferietrekk"), Ferietrekk),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Nyansatt"), Nyansatt),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "NyStilling", gjelderFra = Mock.gjelderFra), NyStilling(gjelderFra = Mock.gjelderFra)),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "NyStillingsprosent", gjelderFra = Mock.gjelderFra), NyStillingsprosent(gjelderFra = Mock.gjelderFra)),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Permisjon", perioder = listOf(Mock.spinnPeriode)), Permisjon(liste = listOf(Mock.periode))),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Permittering", perioder = listOf(Mock.spinnPeriode)), Permittering(liste = listOf(Mock.periode))),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Sykefravaer", perioder = listOf(Mock.spinnPeriode)), Sykefravaer(liste = listOf(Mock.periode))),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "Tariffendring", gjelderFra = Mock.gjelderFra, bleKjent = Mock.bleKjent), Tariffendring(gjelderFra = Mock.gjelderFra, bleKjent = Mock.bleKjent)),
                Pair(Mock.spinnInntektEndringAarsak.copy(aarsak = "VarigLonnsendring", gjelderFra = Mock.gjelderFra), VarigLonnsendring(gjelderFra = Mock.gjelderFra)),
            )
    }

    object Mock {
        val periode = Periode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 30)
        )
        val spinnPeriode = no.nav.syfo.domain.Periode(
            fom = periode.fom,
            tom = periode.tom
        )
        val gjelderFra = LocalDate.of(2021, 1, 1)
        val bleKjent = LocalDate.of(2021, 5, 1)
        val spinnInntektEndringAarsak = SpinnInntektEndringAarsak(aarsak = "Bonus")
    }
    @ParameterizedTest
    @MethodSource("inntektEndringerProvider")
    fun `InntektEndringAarsak mappes til riktig SpinnInntektEndringAarsak`(pair: Pair<SpinnInntektEndringAarsak, InntektEndringAarsak>) {
        val (spinnInntektEndringAarsak, inntektEndringAarsak) = pair
        assertEquals(spinnInntektEndringAarsak, inntektEndringAarsak.tilSpinnInntektEndringAarsak())
    }

    @Test
    fun oversettInntektEndringAarsakTilRapportertInntektEndringAarsak() {
        assertEquals("Permisjon", Permisjon(emptyList()).aarsak())
        assertEquals("Ferie", Ferie(emptyList()).aarsak())
        assertEquals("Ferietrekk", Ferietrekk.aarsak())
        assertEquals("Permittering", Permittering(emptyList()).aarsak())
        assertEquals("Tariffendring", Tariffendring(LocalDate.now(), LocalDate.now()).aarsak())
        assertEquals("VarigLonnsendring", VarigLonnsendring(LocalDate.now()).aarsak())
        assertEquals("NyStilling", NyStilling(LocalDate.now()).aarsak())
        assertEquals("NyStillingsprosent", NyStillingsprosent(LocalDate.now()).aarsak())
        assertEquals("Bonus", Bonus().aarsak())
        assertEquals("Sykefravaer", Sykefravaer(emptyList()).aarsak())
        assertEquals("Nyansatt", Nyansatt.aarsak())
        assertEquals("Feilregistrert", Feilregistrert.aarsak())
    }
}
