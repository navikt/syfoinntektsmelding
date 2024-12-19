package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Bonus
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Feilregistrert
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Ferie
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Ferietrekk
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.InntektEndringAarsak
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.NyStilling
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.NyStillingsprosent
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Nyansatt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Permisjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Permittering
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykefravaer
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Tariffendring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.VarigLoennsendring
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.til
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.date.mai
import no.nav.syfo.domain.inntektsmelding.SpinnInntektEndringAarsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MapInntektEndringAarsakFraSimbaTest {
    companion object {
        // SpinnInntektEndringAarsak som tilsvarer InntektEndringAarsak
        @JvmStatic
        fun inntektEndringerProvider(): List<Pair<SpinnInntektEndringAarsak, InntektEndringAarsak>> {
            val perioder = listOf(1.januar til 31.januar)
            val spinnPerioder =
                perioder.map {
                    no.nav.syfo.domain.Periode(
                        fom = it.fom,
                        tom = it.tom,
                    )
                }
            return listOf(
                SpinnInntektEndringAarsak("Bonus") to Bonus,
                SpinnInntektEndringAarsak("Feilregistrert") to Feilregistrert,
                SpinnInntektEndringAarsak("Ferie", perioder = spinnPerioder) to Ferie(ferier = perioder),
                SpinnInntektEndringAarsak("Ferietrekk") to Ferietrekk,
                SpinnInntektEndringAarsak("Nyansatt") to Nyansatt,
                SpinnInntektEndringAarsak("NyStilling", gjelderFra = 1.januar) to NyStilling(gjelderFra = 1.januar),
                SpinnInntektEndringAarsak("NyStillingsprosent", gjelderFra = 1.januar) to NyStillingsprosent(gjelderFra = 1.januar),
                SpinnInntektEndringAarsak("Permisjon", perioder = spinnPerioder) to Permisjon(permisjoner = perioder),
                SpinnInntektEndringAarsak("Permittering", perioder = spinnPerioder) to Permittering(permitteringer = perioder),
                SpinnInntektEndringAarsak("Sykefravaer", perioder = spinnPerioder) to Sykefravaer(sykefravaer = perioder),
                SpinnInntektEndringAarsak(
                    "Tariffendring",
                    gjelderFra = 1.januar,
                    bleKjent = 1.mai,
                ) to Tariffendring(gjelderFra = 1.januar, bleKjent = 1.mai),
                SpinnInntektEndringAarsak("VarigLonnsendring", gjelderFra = 1.januar) to VarigLoennsendring(gjelderFra = 1.januar),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("inntektEndringerProvider")
    fun `InntektEndringAarsak mappes til riktig SpinnInntektEndringAarsak`(pair: Pair<SpinnInntektEndringAarsak, InntektEndringAarsak>) {
        val (spinnInntektEndringAarsak, inntektEndringAarsak) = pair
        val im =
            lagInntektsmelding().let {
                it.copy(
                    inntekt =
                        it.inntekt?.copy(
                            endringAarsak = inntektEndringAarsak,
                        ),
                )
            }

        val mapped = mapInntektsmelding("1", "2", "3", im)

        assertEquals(spinnInntektEndringAarsak, mapped.rapportertInntekt?.endringAarsakData)
    }
}
