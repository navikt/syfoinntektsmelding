package no.nav.syfo.simba

import junit.framework.TestCase.assertEquals
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.BegrunnelseIngenEllerRedusertUtbetalingKode
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.FullLonnIArbeidsgiverPerioden
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.InntektsmeldingDokument
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.Naturalytelse
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.NaturalytelseKode
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.Periode
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.Refusjon
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.ÅrsakInnsending
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

internal class MapInntektsmeldingKtTest {

    @Test
    fun mapInntektsmeldingMedNaturalytelser() {
        val dato1 = LocalDate.now().minusDays(7)
        val dato2 = LocalDate.now().minusDays(5)
        val dato3 = LocalDate.now().minusDays(3)
        val periode1 = listOf(Periode(dato1, dato1))
        val periode2 = listOf(Periode(dato1, dato1), Periode(dato2, dato2))
        val periode3 = listOf(Periode(dato1, dato3))
        val imDokumentFraSimba = InntektsmeldingDokument(
            orgnrUnderenhet = "123456789",
            identitetsnummer = "12345678901",
            fulltNavn = "Test testesen",
            virksomhetNavn = "Blåbærsyltetøy A/S",
            behandlingsdager = listOf(dato1),
            egenmeldingsperioder = periode1,
            bestemmendeFraværsdag = dato2,
            fraværsperioder = periode2,
            arbeidsgiverperioder = periode3,
            beregnetInntekt = BigDecimal.valueOf(100_000L),
            fullLønnIArbeidsgiverPerioden = FullLonnIArbeidsgiverPerioden(utbetalerFullLønn = false, begrunnelse = BegrunnelseIngenEllerRedusertUtbetalingKode.BESKJED_GITT_FOR_SENT),
            refusjon = Refusjon(true, BigDecimal.TEN, null, null),
            naturalytelser = listOf(
                Naturalytelse(
                    NaturalytelseKode
                        .INNBETALINGTILUTENLANDSKPENSJONSORDNING,
                    dato1, BigDecimal.valueOf(10_000L)
                ),
                Naturalytelse(
                    NaturalytelseKode.BIL, dato2, BigDecimal.valueOf(5_000L)
                )
            ),
            tidspunkt = OffsetDateTime.now(),
            årsakInnsending = ÅrsakInnsending.NY,
            identitetsnummerInnsender = "123456789"
        )
        val mapped = mapInntektsmelding("1323", "sdfds", "134", imDokumentFraSimba)
        val naturalytelse = mapped.opphørAvNaturalYtelse.get(0)
        assertEquals(no.nav.syfo.domain.inntektsmelding.Naturalytelse.INNBETALINGTILUTENLANDSKPENSJONSORDNING, naturalytelse.naturalytelse)
    }
}
